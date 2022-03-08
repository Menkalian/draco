package de.menkalian.draco.data.game.values

import de.menkalian.draco.data.game.values.TransferableValue.TransferableValueType.BINARY
import de.menkalian.draco.data.game.values.TransferableValue.TransferableValueType.BOOLEAN
import de.menkalian.draco.data.game.values.TransferableValue.TransferableValueType.DOUBLE
import de.menkalian.draco.data.game.values.TransferableValue.TransferableValueType.INTEGER
import de.menkalian.draco.data.game.values.TransferableValue.TransferableValueType.LONG
import de.menkalian.draco.data.game.values.TransferableValue.TransferableValueType.STRING
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Class to encapsulate basic data types as `String`, so they can be stored in the same data structure (e.g. `Map<String, TransferableValue>`).
 * This class is also serializable with the `kotlinx.serialization` library.
 *
 * @constructor Creates an instance from the given value. It is **not** recommended using the constructor directly. You should use the various `from` methods instead.
 */
@kotlinx.serialization.Serializable
data class TransferableValue(val type: TransferableValueType, val value: String) {
    companion object {
        /**
         * Creates an instance with type `TransferableValue.BOOLEAN`
         */
        fun from(boolean: Boolean) = TransferableValue(BOOLEAN, boolean.toString())

        /**
         * Creates an instance with type `TransferableValue.STRING`
         */
        fun from(str: String) = TransferableValue(STRING, str)

        /**
         * Creates an instance with type `TransferableValue.INTEGER`
         */
        fun from(i: Int) = TransferableValue(INTEGER, i.toString())

        /**
         * Creates an instance with type `TransferableValue.LONG`
         */
        fun from(l: Long) = TransferableValue(LONG, l.toString())

        /**
         * Creates an instance with type `TransferableValue.DOUBLE`
         */
        fun from(d: Double) = TransferableValue(DOUBLE, d.toString())

        /**
         * Creates an instance with type `TransferableValue.BINARY`
         */
        fun from(bytes: ByteArray) = TransferableValue(BINARY, bytes.toHexString())

        /**
         * Represents the content of an `ByteArray` as a hexadecimal String
         */
        private fun ByteArray.toHexString(): String {
            return joinToString("") { it.toUByte().toString(16) }
        }

        /**
         * Converts this (hexadecimal) String to an array of bytes.
         */
        private fun String.parseHex(): ByteArray {
            val toParse = if (length % 2 == 1) {
                "0$this"
            } else {
                this
            }

            return toParse.chunked(2).map { it.toInt(16).toByte() }.toTypedArray().toByteArray()
        }
    }

    /**
     * Returns the String value of this object.
     * This is identical to the representation of the value which is stored.
     */
    override fun toString() = value

    /**
     * Tries to interpret this value as Boolean and returns it.
     */
    fun toBoolean(strict: Boolean = false): Boolean {
        if (strict) {
            return when (type) {
                BOOLEAN -> value.toBooleanStrict()
                else    -> {
                    if (value == "1")
                        true
                    else if (value == "0")
                        false
                    else
                        throw RuntimeException("No strict conversion to boolean available")
                }
            }
        } else {
            return when (type) {
                BOOLEAN -> value.toBoolean()
                STRING  -> value.toBoolean()
                BINARY  -> toByteArray().any { it.toInt() != 0 }
                INTEGER -> toInt() != 0
                LONG    -> toLong() != 0L
                DOUBLE  -> toDouble() != 0.0 && !toDouble().isNaN()
            }
        }
    }

    /**
     * Tries to interpret this value as Integer and returns it.
     *
     * @throws UnsupportedOperationException if the value can not be converted to an Integer
     */
    fun toInt(): Int =
        when (type) {
            BOOLEAN -> if (toBoolean()) 1 else 0
            STRING  -> value.toInt()
            BINARY  -> value.substring(max(0, value.length - 8)).toInt(16)
            INTEGER -> value.toInt()
            LONG    -> toLong().toInt()
            DOUBLE  -> toDouble().roundToInt()
        }

    /**
     * Tries to interpret this value as Long and returns it.
     *
     * @throws UnsupportedOperationException if the value can not be converted to a Long
     */
    fun toLong(): Long =
        when (type) {
            BOOLEAN -> if (toBoolean()) 1 else 0
            STRING  -> value.toLong()
            BINARY  -> value.substring(max(0, value.length - 16)).toLong(16)
            INTEGER -> value.toLong()
            LONG    -> value.toLong()
            DOUBLE  -> toDouble().roundToLong()
        }

    /**
     * Tries to interpret this value as Double and returns it.
     *
     * @throws UnsupportedOperationException if the value can not be converted to a Double
     */
    fun toDouble(): Double =
        when (type) {
            BOOLEAN -> if (toBoolean()) 1.0 else 0.0
            STRING  -> value.toDouble()
            INTEGER -> value.toDouble()
            DOUBLE  -> value.toDouble()
            LONG    -> toLong().toDouble()
            BINARY  -> Double.fromBits(toLong())
        }

    /**
     * Returns the binary representation of this value.
     *
     * @throws UnsupportedOperationException if there is no binary representation
     */
    fun toByteArray(): ByteArray =
        when (type) {
            BOOLEAN -> if (toBoolean()) arrayOf(1.toByte()).toByteArray() else arrayOf(0.toByte()).toByteArray()
            STRING  -> value.encodeToByteArray()
            BINARY  -> value.parseHex()
            INTEGER -> (3 downTo 0).map { toInt().shr(8 * it).toByte() }.toByteArray()
            LONG    -> (7 downTo 0).map { toLong().shr(8 * it).toByte() }.toByteArray()
            DOUBLE  -> (7 downTo 0).map { toDouble().toRawBits().shr(8 * it).toByte() }.toByteArray()
        }

    /**
     * Defines all possible types which can be represented in an object
     *
     * @property BOOLEAN True/False value. Saved by it's string value
     * @property STRING Plain String/Text. The saved representation is identical to the value
     * @property BINARY Raw/Binary data. It is saved as hexadecimal string.
     * @property INTEGER 32-bit Integer. Saved as decimal string.
     * @property LONG 64-bit Integer. Saved as decimal string.
     * @property DOUBLE 64-bit floating point. Saved as decimal string.
     */
    enum class TransferableValueType {
        BOOLEAN,
        STRING,
        BINARY,
        INTEGER,
        LONG,
        DOUBLE
    }
}
