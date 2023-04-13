package ru.sberbank.blockchain.cnft.gate.service

import tools.http.service.annotations.HttpGet

import scala.language.higherKinds

trait POWServiceSpec[R[_]] {

    @HttpGet("/pow-difficulty")
    def powDifficulty: R[Int]

}