Feature: Burn Locked

  Scenario: Burn Locked Before Date
    Given There is a client "SBER"
    Given There is a client "Client1"
    When "SBER" registering datafeed "TestFeed1"
    When "SBER" submitted value "2022-02-22T09:00:00.0Z" for datafeed "TestFeed1"

    When "SBER" registered token type "SBC" with burn locked "before" and date "2022-02-23T09:00:00.0Z" with datafeed "TestFeed1"
    When "SBER" issued token "T1" for "Client1" of type "SBC" with value "100"
    When "Client1" sees one token in his list
    When "SBER" sees no tokens in his list
    When "Client1" not able to burn first token in his tokens list
    When "Client1" sees one token in his list
    When "SBER" submitted value "2022-02-24T09:00:00.0Z" for datafeed "TestFeed1"
    When "Client1" burns his first token in his tokens list
    When "Client1" sees no tokens in his list


#  Scenario: Burn Locked After Date
#    Given There is a client "SBER"
#    Given There is a client "Client1"
#    When "SBER" registering datafeed "TestFeed1"
#    When "SBER" submitted value "2022-02-22T09:00:00.0Z" for datafeed "TestFeed1"
#
#    When "SBER" registered token type "SBC" with burn locked "after" and date "2022-02-23T09:00:00.0Z" with datafeed "TestFeed1"
#    When "SBER" issued token "T1" for "Client1" of type "SBC" with value "100"
#    When "Client1" sees one token in his list
#    When "SBER" sees no tokens in his list
#    When "SBER" submitted value "2022-02-24T09:00:00.0Z" for datafeed "TestFeed1"
#    When "Client1" not able to burn first token in his tokens list
#    When "Client1" sees one token in his list
#
#  Scenario: Burn Locked Between two Dates (After)
#    Given There is a client "SBER"
#    Given There is a client "Client1"
#    When "SBER" registering datafeed "TestFeed1"
#    When "SBER" submitted value "2022-02-22T09:00:00.0Z" for datafeed "TestFeed1"
#
#    When "SBER" registered token type "SBC" with burn locked between date "2022-02-23T09:00:00.0Z" and date "2022-02-24T09:00:00.0Z" with datafeed "TestFeed1"
#    When "SBER" issued token "T1" for "Client1" of type "SBC" with value "100"
#    When "Client1" sees one token in his list
#    When "SBER" sees no tokens in his list
#
#    When "SBER" submitted value "2022-02-23T19:00:00.0Z" for datafeed "TestFeed1"
#    When "Client1" not able to burn first token in his tokens list
#    When "Client1" sees one token in his list
#
#    When "SBER" submitted value "2022-02-24T10:00:00.0Z" for datafeed "TestFeed1"
#    When "Client1" burns his first token in his tokens list
#    When "Client1" sees no tokens in his list

  Scenario: Burn Locked Betweed two Dates (Before)
    Given There is a client "SBER"
    Given There is a client "Client1"
    When "SBER" registering datafeed "TestFeed1"
    When "SBER" submitted value "2022-02-22T09:00:00.0Z" for datafeed "TestFeed1"

    When "SBER" registered token type "SBC" with burn locked between date "2022-02-23T09:00:00.0Z" and date "2022-02-24T09:00:00.0Z" with datafeed "TestFeed1"
    When "SBER" issued token "T1" for "Client1" of type "SBC" with value "100"
    When "Client1" sees one token in his list
    When "SBER" sees no tokens in his list

    When "SBER" submitted value "2022-02-23T08:00:00.0Z" for datafeed "TestFeed1"
    When "Client1" burns his first token in his tokens list
    When "Client1" sees no tokens in his list
