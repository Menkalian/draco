package de.menkalian.draco.server.util

import de.menkalian.draco.data.game.values.TransferableValue
import de.menkalian.draco.data.game.values.Values
import org.apache.tomcat.util.codec.binary.Base64
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.security.MessageDigest

fun Any.logger(): Logger {
    return LoggerFactory.getLogger(this::class.java)
}

fun ByteArray.sha512(): ByteArray {
    val md = MessageDigest.getInstance("SHA-512")
    return md.digest(this)
}

fun String.authTokenHash() = Base64
    .encodeBase64String(
        Base64
            .decodeBase64(this)
            .sha512()
    )

fun Values.addStringList(
    list: List<String>,
    amountKey: String,
    valueKeySupplier: (Int) -> String
) {
    this[amountKey] = TransferableValue.from(list.size)
    list.forEachIndexed { idx, s ->
        this[valueKeySupplier(idx + 1)] = TransferableValue.from(s)
    }
}