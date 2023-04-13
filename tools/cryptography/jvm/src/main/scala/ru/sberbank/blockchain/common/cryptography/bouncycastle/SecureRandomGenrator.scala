package ru.sberbank.blockchain.common.cryptography.bouncycastle

import ru.sberbank.blockchain.cnft.commons.{Bytes, asBytes}
import ru.sberbank.blockchain.common.cryptography.SecureRandomGenerator

import java.security.SecureRandom

object JSecureRandomGenerator extends SecureRandomGenerator {

    private val secureGenerator = new SecureRandom()

    override def nextInt(): Int = secureGenerator.nextInt()

    override def nextBytes(num: Int): Bytes = {
        val a = new Array[Byte](num)
        secureGenerator.nextBytes(a)
        asBytes(a)
    }
}
