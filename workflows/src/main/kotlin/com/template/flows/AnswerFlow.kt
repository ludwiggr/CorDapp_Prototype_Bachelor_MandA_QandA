package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.QAContract
import com.template.states.QAState
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

/** COMMENT */
// Comment

/**
 * See the constructor, we need to give an answer and the externalId of the question we want to answer
 */
@InitiatingFlow
@StartableByRPC
class AnswerFlow(
        private val answer: String,
        private val linearId: UniqueIdentifier
) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {
        // Initiator flow logic goes here.

        // Now we want to retrieve our question state
        // Create a query criteria, we give list of externalIds we want to retrieve, we only want one
        val criteria = QueryCriteria.LinearStateQueryCriteria(
                linearId = listOf(linearId))

        // Execute the query
        val stateAndRef = serviceHub.vaultService.queryBy<QAState>(criteria).states.first()

        // Get the state
        val inputState = stateAndRef.state.data

        // Check that questioner is not ourselfes before intitating flow
        requireThat {"The questioner should be different from our identity" using  (inputState.questioner != ourIdentity) }

        // We change the answer
        val outputState = inputState.copy(answer = answer)

        // Answer command, the signers are both participants
        val answerCommand = Command(QAContract.Commands.Answer(), outputState.participants.map { it.owningKey })

        // Create a new TransactionBuilder object, the notary we'll use is the one who notarised the input state
        val builder = TransactionBuilder(stateAndRef.state.notary)

        // Add Input and output
        // builder = changes transaction state
        builder.addOutputState(outputState, QAContract.ID)
        builder.addInputState(stateAndRef)
        builder.addCommand(answerCommand)

        // Verify and add our signature
        builder.verify(serviceHub)
        val ptx = serviceHub.signInitialTransaction(builder)

        // We initiate a session flow with the questioner.
        val questionerParty = inputState.questioner
        val questionerSession = initiateFlow(questionerParty)

        // Send it to him to add his signature
        val fullySignexTx = subFlow(CollectSignaturesFlow(ptx, listOf(questionerSession)))

        // Call finality flow, will send it to notary then saved in our vault.
        // We give a list of session with parties we need to send it to them
        return subFlow(FinalityFlow(fullySignexTx, questionerSession))
    }
}


@InitiatedBy(AnswerFlow::class)
class AnswerFlowResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {
        val signedTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                // Here is some business logic verification, i.e I don't accept this kind of answers,
                val output = stx.tx.outputsOfType(QAState::class.java).first()
                "This kind of answer is not accepted." using !output.answer!!.contains("sorry", true)
                "This kind of answer is not accepted." using !output.answer!!.contains("I don't know", true)

            }
        }

        // Get partially signed signature and sign it
        val txWeJustSignedId = subFlow(signedTransactionFlow)

        // We accept the transaction to be saved
        subFlow(ReceiveFinalityFlow(counterpartySession, txWeJustSignedId.id))
    }
}
