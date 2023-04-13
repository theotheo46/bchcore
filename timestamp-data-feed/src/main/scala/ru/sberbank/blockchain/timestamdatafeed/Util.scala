package ru.sberbank.blockchain.timestamdatafeed

object Util {
    def environmentMandatory(name: String): String =
        sys.env.getOrElse(name,
            throw new Exception(s"Mandatory environment variable $name is missing.")
        )

    def environmentOptional(name: String, default: String): String =
        sys.env.getOrElse(name, default)
}