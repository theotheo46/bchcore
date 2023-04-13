package ru.sberbank.blockchain.cnft.example

object Util {
    def environmentMandatory(name: String): String =
        sys.env.getOrElse(name,
            throw new Exception(s"Mandatory environment variable $name is missing.")
        )
}
