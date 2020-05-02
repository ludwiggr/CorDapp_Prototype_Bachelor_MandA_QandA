package com.template.contracts

import com.template.states.QAState
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

/** COMMENT */
// Comment

// ************
// * Contract *
// ************
class QAContract : Contract {
    companion object {
        // Used to identify the contract when building a transaction.
        const val ID = "com.template.contracts.QAContract"
    }

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        // This means we can only accept transactions of type Ask and Answer
        val command = tx.commands.requireSingleCommand<Commands>()

        // Logic to pass for Ask Command
        when (command.value) {
            is Commands.Ask -> requireThat {
                "No inputs should be consumed when issuing a QA Contract." using (tx.inputs.isEmpty())
                "Only one output state should be created when issuing a Question." using (tx.outputs.size == 1)
                val output = tx.outputsOfType<QAState>().single()
                "A new QA State must have a non-empty question." using (output.question.isNotEmpty())
                "A new QA State must have an empty answer." using (output.answer == null)
                "The Questioner and Respondent cannot have the same identity." using (output.questioner != output.respondent)
                "Questioner and Respondent must sign QA Question issue." using (command.signers.toSet() == setOf(output.questioner.owningKey, output.respondent.owningKey))
            }

            // Logic to pass for Answer Command
            is Commands.Answer -> requireThat {
                "An Answer transaction should only consume one input state." using (tx.inputs.size == 1)
                "An Answer transaction should only create one output state." using (tx.outputs.size == 1)
                val input = tx.inputsOfType<QAState>().single()
                "The QA State must have a non-empty question." using (input.question.isNotEmpty())
                "The QA State must have an empty answer." using (input.answer == null)
//                "The Question has already been answered." using (input.answer!!.isEmpty())
                val output = tx.outputsOfType<QAState>().single()
                "A Answer can't be No Idea." using (output.answer != "No Idea")
                "A new Answer must have a non-empty question." using (output.question.isNotEmpty())
                "A new Answer must have a non-empty answer." using output.answer!!.isNotEmpty()
                val qas = tx.groupStates<QAState, String> { it.linearId.toString() }.single()
                "There must be one input QA." using (qas.inputs.size == 1)
                "There must be one outpu QA." using (qas.outputs.size == 1)


                // You can add any validation you want, like verifying that only the answer has changed and nothing else etc
                // using: if expression is false then throw error
                "The Respondent property must change in a transfer." using (input.respondent == output.respondent)
                "The Questioner and Respondent must sign an QA transaction" using
                        (command.signers.toSet() == (input.participants.map { it.owningKey }.toSet()))
            }
        }
    }

    // Used to indicate the transaction's intent.
    // There was "TypeOnlyCommandData" before, it's not necessary, but still works if u put it
    interface Commands : CommandData {
        class Ask : Commands
        class Answer : Commands
    }
}