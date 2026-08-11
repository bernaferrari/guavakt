package dev.guavakt.reflect

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TypeTokenJvmHierarchyTest {
    open class Animal
    class Dog : Animal()

    @Test fun subtypeHierarchyUsesJvmAssignability() {
        val dog = TypeToken.of(Dog::class)
        val animal = TypeToken.of(Animal::class)
        assertTrue(dog.isSubtypeOf(animal))
        assertTrue(animal.isSupertypeOf(dog))
        assertFalse(animal.isSubtypeOf(dog))
    }

    @Test fun subtypeAndSupertypeValidateJvmHierarchy() {
        val animal = TypeToken.of(Animal::class)
        val dog = animal.getSubtype(Dog::class)
        assertEquals(Dog::class, dog.getRawType())
        assertEquals(Animal::class, dog.getSupertype(Animal::class).getRawType())
        assertFailsWith<IllegalArgumentException> { animal.getSubtype(String::class) }
    }
}
