package ru.sberbank.blockchain.cnft.gate.service

import ru.sberbank.blockchain.cnft.commons.Result

trait POWService extends POWServiceSpec[Result] {
    def POWDifficulty: Int

    override def powDifficulty: Result[Int] = Result(POWDifficulty)
}