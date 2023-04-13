package ru.sberbank.blockchain.cnft.engine.dna

import ru.sberbank.blockchain.cnft.common.types.Result

import java.time.OffsetDateTime

trait LockUpByDateGene {

    def lockUpPassed(context: GeneExecutionContext, feedId: String, lockupDate: String): Result[Boolean] =
        for {
            threshold <- Result(OffsetDateTime.parse(lockupDate).toInstant)
            feedValue <- context.getFeedValue(feedId)
            feedDate <- Result(feedValue.content.head)
            current <- Result(OffsetDateTime.parse(feedDate).toInstant)
        }
        yield current.compareTo(threshold) > 0

    def lockUpPassed(context: GeneExecutionContext, feedId:String, lockupDateFrom: String, lockupDateUntil: String): Result[Boolean] =
        for {
            from <- Result(OffsetDateTime.parse(lockupDateFrom))
            until <- Result(OffsetDateTime.parse(lockupDateUntil))
            feedValue <- context.getFeedValue(feedId)
            feedDate <- Result(feedValue.content.head)
            current <- Result(OffsetDateTime.parse(feedDate))
        } yield current.isBefore(from) || current.isAfter(until)

    def lockUpPassed(context: GeneExecutionContext): Result[Boolean] = {
        val parameters = context.geneParameters
        if (parameters.length == 2) {
            val Array(feedId, date) = context.geneParameters
            lockUpPassed(context, feedId, date)
        }
        else if (parameters.length == 3) {
            val Array(feedId, dateFrom, dateUntil) = context.geneParameters
            lockUpPassed(context, feedId, dateFrom, dateUntil)
        }
        else Result.Fail("Invalid gene parameters")
    }

}
