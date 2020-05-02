package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.QAContract
import com.template.states.QAState
import net.corda.core.contracts.Command
import net.corda.core.contracts.Requirements.using
import net.corda.core.contracts.requireThat
import net.corda.core.crypto.random63BitValue
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

/** COMMENT */
// Comment

/**
 * The constructor is the only information the questioner needs to be able to start this flow,
 * a new QAState will be created after (A custom externalId can be assigned too).
 */


@InitiatingFlow
@StartableByRPC
class AskFlow(
        private val question: String,
        private val respondent: Party) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {
        // Check that respondent is not ourselfes before intitating flow
        requireThat {"The respondent should be different from our identity" using  (respondent != ourIdentity) }

        // That's how we get our identity
        val questioner: Party = ourIdentity

        // Create QAState, here a random externalId was generated, but one can make it custom if it needs to be
        val outputState: QAState = QAState(question, questioner, respondent)

        // Get the first operating Notary Identity from the Network Map
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        // Ask command is called, the signers are both the questioner and the respondent
        val askCommand = Command(QAContract.Commands.Ask(), listOf(questioner.owningKey, respondent.owningKey))

        // Create a new TransactionBuilder object and add QA State as output
        // Here, the creation of the new State is initiated
        val builder = TransactionBuilder(notary = notary)
                .addCommand(askCommand)
                .addOutputState(outputState, QAContract.ID)

        // Verify and add our signature
        // Builder = changing the transaction state
        builder.verify(serviceHub)
        val ptx = serviceHub.signInitialTransaction(builder)

        // We initiate a Subflow with the respondent. (see Subflow AskFlowResponder below)
        val respondentSession = initiateFlow(respondent)

        val fullySignedTransaction = subFlow(CollectSignaturesFlow(ptx, listOf(respondentSession)))

        // Call finality flow, Transaction will be saved in our vault.
        // We give a list of session with parties we need to send it to them
        return subFlow(FinalityFlow(fullySignedTransaction, respondentSession))
    }
}

@InitiatedBy(AskFlow::class)
class AskFlowResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {
        val signedTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
               //requirement
                val output = stx.tx.outputsOfType(QAState::class.java).first()
                "This kind of question is not accepted." using !output.question.contains("No Question", true)

            }
        }

        // Get partially signed signature and sign it
        val txWeJustSignedId = subFlow(signedTransactionFlow)

        // We accept the transaction, save it without signing
        subFlow(ReceiveFinalityFlow(counterpartySession, txWeJustSignedId.id))
    }
}
