Feature: Endorsement functionality

  Scenario: Positive scenario
    Given There is a client "SBER"
    Given There is a client "Client"
    When "SBER" approves "Client" endorsement
    When "Client" checks his endorsements

  Scenario: Negative scenario
    Given There is a client "SBER"
    Given There is a client "Client"
    When "SBER" rejects "Client" endorsement
    When "Client" checks his endorsements are empty


  Scenario: Request endorsement
    Given There is a client "SBER"
    Given There is a client "Client"
    Given "Client" asks "SBER" for endorsement
    When "SBER" approves "Client" endorsement
    When "Client" checks his endorsements
