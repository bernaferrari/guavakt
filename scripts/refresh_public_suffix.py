#!/usr/bin/env python3
"""Audit and regenerate GuavaKt's compact public-suffix trie from the official PSL."""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
import urllib.request
from collections import deque
from dataclasses import dataclass, field
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
CANONICAL_URL = "https://publicsuffix.org/list/public_suffix_list.dat"
DEFAULT_PATTERNS = ROOT / "guavakt-net/src/commonMain/kotlin/com/google/thirdparty/publicsuffix/PublicSuffixPatterns.kt"
DEFAULT_LOCK = ROOT / "public_suffix.lock.json"
CHUNK_SIZE = 1 << 13
TYPE_BITS = {"REGISTRY": 1, "PRIVATE": 2}
BITS_TYPE = {1: "REGISTRY", 2: "PRIVATE"}


@dataclass(frozen=True, order=True)
class Rule:
    kind: str
    domain: str
    suffix_type: str | None


@dataclass
class Node:
    label: str
    children: dict[str, "Node"] = field(default_factory=dict)
    exact_type: str | None = None
    wildcard_type: str | None = None
    exclusion: bool = False


def fetch(url: str) -> bytes:
    request = urllib.request.Request(url, headers={"User-Agent": "GuavaKt PSL refresh tool"})
    with urllib.request.urlopen(request, timeout=30) as response:
        return response.read()


def public_suffix_rules(data: bytes) -> tuple[dict[str, object], set[Rule]]:
    text = data.decode("utf-8")
    current_type: str | None = None
    rules: set[Rule] = set()
    for source_line in text.splitlines():
        line = source_line.strip()
        if line == "// ===BEGIN ICANN DOMAINS===":
            current_type = "REGISTRY"
        elif line == "// ===BEGIN PRIVATE DOMAINS===":
            current_type = "PRIVATE"
        elif line.startswith("//") or not line:
            continue
        elif current_type is None:
            raise ValueError(f"rule before a PSL section: {source_line!r}")
        elif line.startswith("!"):
            # The compact format stores exception behavior without a separate type bit.
            rules.add(Rule("exception", line[1:], None))
        elif line.startswith("*."):
            rules.add(Rule("wildcard", line[2:], current_type))
        else:
            rules.add(Rule("exact", line, current_type))
    metadata = {
        "url": CANONICAL_URL,
        "version": next((line.removeprefix("// VERSION: ") for line in text.splitlines() if line.startswith("// VERSION: ")), None),
        "commit": next((line.removeprefix("// COMMIT: ") for line in text.splitlines() if line.startswith("// COMMIT: ")), None),
        "sha256": hashlib.sha256(data).hexdigest(),
        "rules": len(rules),
    }
    return metadata, rules


def kotlin_chunks(source: str, property_name: str, next_property: str) -> list[str]:
    marker = f"val {property_name}: List<String> = listOf("
    try:
        section = source.split(marker, 1)[1].split(f"    )\n\n    {next_property}", 1)[0]
    except IndexError as error:
        raise ValueError(f"could not locate {property_name} in generated patterns") from error
    literals = re.findall(r'"(?:\\.|[^"\\])*"', section)
    if not literals:
        raise ValueError(f"{property_name} has no Kotlin string literals")
    try:
        return [json.loads(literal) for literal in literals]
    except json.JSONDecodeError as error:
        raise ValueError(f"could not decode {property_name} literals") from error


def patterns_rules(patterns_path: Path) -> set[Rule]:
    source = patterns_path.read_text(encoding="utf-8")
    pool = "".join(kotlin_chunks(source, "STRING_POOL", "val TRIE_DATA"))
    trie = "".join(kotlin_chunks(source, "TRIE_DATA", "const val CHUNK_SHIFT"))
    if len(trie) % 3:
        raise ValueError("trie data length is not a whole number of nodes")

    def label_at(offset: int) -> str:
        length = ord(pool[offset])
        return pool[offset + 1 : offset + 1 + length]

    rules: set[Rule] = set()
    todo: deque[tuple[int, tuple[str, ...]]] = deque([(0, ())])
    seen: set[int] = set()
    while todo:
        index, labels = todo.popleft()
        if index in seen:
            raise ValueError(f"trie references node {index} twice")
        seen.add(index)
        node_offset = index * 3
        label_offset = ord(trie[node_offset])
        metadata = ord(trie[node_offset + 2])
        if index != 0:
            labels = (*labels, label_at(label_offset))
        domain = ".".join(reversed(labels))
        exact = BITS_TYPE.get((metadata >> 14) & 0x3)
        wildcard = BITS_TYPE.get((metadata >> 12) & 0x3)
        if exact is not None:
            rules.add(Rule("exact", domain, exact))
        if wildcard is not None:
            rules.add(Rule("wildcard", domain, wildcard))
        if metadata & (1 << 11):
            rules.add(Rule("exception", domain, None))
        first_child = ord(trie[node_offset + 1])
        child_count = metadata & 0x7FF
        for child in range(first_child, first_child + child_count):
            todo.append((child, labels))
    return rules


def rules_digest(rules: set[Rule]) -> str:
    payload = "\n".join(
        f"{rule.kind}\t{rule.domain}\t{rule.suffix_type or ''}" for rule in sorted(rules)
    ).encode("utf-8")
    return hashlib.sha256(payload).hexdigest()


def runtime_metadata(patterns_path: Path, rules: set[Rule]) -> dict[str, object]:
    try:
        display_path = str(patterns_path.resolve().relative_to(ROOT))
    except ValueError:
        # A staging directory is the safest way to inspect a generated refresh before replacing
        # the checked-in source, so it must be a supported output location.
        display_path = str(patterns_path)
    return {
        "patterns": display_path,
        "source_sha256": hashlib.sha256(patterns_path.read_bytes()).hexdigest(),
        "rules": len(rules),
        "rules_sha256": rules_digest(rules),
    }


def print_status(runtime: dict[str, object], upstream: dict[str, object], missing: set[Rule], extra: set[Rule]) -> None:
    print(f"RUNTIME_SOURCE_SHA256={runtime['source_sha256']}")
    print(f"RUNTIME_RULES={runtime['rules']}")
    print(f"RUNTIME_RULES_SHA256={runtime['rules_sha256']}")
    print(f"PSL_VERSION={upstream['version']}")
    print(f"PSL_COMMIT={upstream['commit']}")
    print(f"PSL_SHA256={upstream['sha256']}")
    print(f"PSL_RULES={upstream['rules']}")
    print(f"MISSING_FROM_RUNTIME={len(missing)}")
    print(f"EXTRA_IN_RUNTIME={len(extra)}")
    print("STATUS=CURRENT" if not missing and not extra else "STATUS=REFRESH_REQUIRED")
    for name, current_rules in (("MISSING_SAMPLE", missing), ("EXTRA_SAMPLE", extra)):
        sample = ", ".join(
            ("!" if rule.kind == "exception" else "*." if rule.kind == "wildcard" else "") + rule.domain
            for rule in sorted(current_rules)[:5]
        )
        if sample:
            print(f"{name}={sample}")


def build_tree(rules: set[Rule]) -> Node:
    root = Node("")
    for rule in rules:
        node = root
        for label in reversed(rule.domain.split(".")):
            node = node.children.setdefault(label, Node(label))
        if rule.kind == "exact":
            node.exact_type = rule.suffix_type
        elif rule.kind == "wildcard":
            node.wildcard_type = rule.suffix_type
        else:
            node.exclusion = True
    return root


def chunks(value: str) -> list[str]:
    return [value[index : index + CHUNK_SIZE] for index in range(0, len(value), CHUNK_SIZE)] or [""]


def kotlin_string(value: str) -> str:
    escaped: list[str] = []
    for character in value:
        code = ord(character)
        if character == "\\":
            escaped.append("\\\\")
        elif character == '"':
            escaped.append('\\"')
        elif character == "$":
            escaped.append("\\$")
        elif character == "\n":
            escaped.append("\\n")
        elif character == "\r":
            escaped.append("\\r")
        elif character == "\t":
            escaped.append("\\t")
        elif 0x20 <= code <= 0x7E:
            escaped.append(character)
        elif code <= 0xFFFF:
            escaped.append(f"\\u{code:04x}")
        else:
            code -= 0x10000
            escaped.append(f"\\u{0xD800 + (code >> 10):04x}\\u{0xDC00 + (code & 0x3FF):04x}")
    return '"' + "".join(escaped) + '"'


def render_patterns(rules: set[Rule], upstream: dict[str, object]) -> str:
    root = build_tree(rules)
    order: list[Node] = [root]
    queue: deque[Node] = deque([root])
    first_children: dict[int, tuple[int, int]] = {}
    while queue:
        node = queue.popleft()
        children = [node.children[label] for label in sorted(node.children)]
        first_children[id(node)] = (len(order), len(children))
        order.extend(children)
        queue.extend(children)

    labels = sorted({node.label for node in order if node.label})
    offsets: dict[str, int] = {"": 0}
    string_pool = "\x00"
    for label in labels:
        if len(label) > 0xFFFF:
            raise ValueError(f"PSL label is too long for the compact format: {label!r}")
        offsets[label] = len(string_pool)
        string_pool += chr(len(label)) + label

    trie_chars: list[str] = []
    for node in order:
        first_child, child_count = first_children[id(node)]
        if child_count > 0x7FF:
            raise ValueError(f"node {node.label!r} has too many children for the compact format")
        metadata = child_count
        if node.exclusion:
            metadata |= 1 << 11
        if node.wildcard_type is not None:
            metadata |= TYPE_BITS[node.wildcard_type] << 12
        if node.exact_type is not None:
            metadata |= TYPE_BITS[node.exact_type] << 14
        trie_chars.extend((chr(offsets[node.label]), chr(first_child), chr(metadata)))

    def render_list(name: str, values: list[str]) -> str:
        body = ",\n".join(f"        {kotlin_string(value)}" for value in values)
        return f"    val {name}: List<String> = listOf(\n{body}\n    )"

    header = (
        "// GENERATED FILE - DO NOT EDIT.\n"
        f"// Source: {upstream['url']}\n"
        f"// VERSION: {upstream['version']}\n"
        f"// COMMIT: {upstream['commit']}\n"
        f"// SHA256: {upstream['sha256']}\n\n"
        "package dev.guavakt.thirdparty.publicsuffix\n\n"
        "/** Compact, generated public-suffix trie. See docs/public-suffix.md for refresh discipline. */\n"
        "object PublicSuffixPatterns {\n"
    )
    return (
        header
        + render_list("STRING_POOL", chunks(string_pool))
        + "\n\n"
        + render_list("TRIE_DATA", chunks("".join(trie_chars)))
        + "\n\n"
        + "    const val CHUNK_SHIFT: Int = 13\n\n"
        + "    val TRIE: PublicSuffixTrie by lazy {\n"
        + "        PublicSuffixTrie(TRIE_DATA, STRING_POOL, CHUNK_SHIFT)\n"
        + "    }\n"
        + "}\n"
    )


def write_lock(path: Path, runtime: dict[str, object], upstream: dict[str, object]) -> None:
    payload = {"format": 1, "runtime": runtime, "upstream": upstream}
    path.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=("status", "check", "generate"))
    parser.add_argument("--url", default=CANONICAL_URL)
    parser.add_argument("--patterns", type=Path, default=DEFAULT_PATTERNS)
    parser.add_argument("--output", type=Path, help="generated Kotlin output (required for generate)")
    parser.add_argument("--lock-output", type=Path, default=DEFAULT_LOCK)
    args = parser.parse_args()

    data = fetch(args.url)
    upstream, official_rules = public_suffix_rules(data)
    upstream["url"] = args.url
    if args.command == "generate":
        if args.output is None:
            parser.error("generate requires --output; this tool never overwrites source implicitly")
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(render_patterns(official_rules, upstream), encoding="utf-8")
        generated_rules = patterns_rules(args.output)
        if generated_rules != official_rules:
            raise AssertionError("generated trie does not round-trip to the official rule set")
        runtime = runtime_metadata(args.output, generated_rules)
        write_lock(args.lock_output, runtime, upstream)
        print(f"GENERATED_PATTERNS={args.output}")
        print(f"GENERATED_LOCK={args.lock_output}")
        print_status(runtime, upstream, set(), set())
        return 0

    runtime_rules = patterns_rules(args.patterns)
    runtime = runtime_metadata(args.patterns, runtime_rules)
    missing = official_rules - runtime_rules
    extra = runtime_rules - official_rules
    print_status(runtime, upstream, missing, extra)
    if args.command == "check" and (missing or extra):
        print("Run the reviewed generate command documented in docs/public-suffix.md.", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
