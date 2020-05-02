package com.template//package com.template

import com.template.flows.AnswerFlow
import com.template.states.QAState
import com.template.flows.AnswerFlowResponder
import com.template.flows.AskFlow
import com.template.flows.AskFlowResponder
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.getOrThrow
import net.corda.testing.internal.chooseIdentityAndCert
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test


class AnswerFlowTests {
    private val network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
            TestCordapp.findCordapp("com.template.contracts"),
            TestCordapp.findCordapp("com.template.flows")
    )))
    private val questionerNode = network.createNode()
    private val respondentNode = network.createNode(CordaX500Name.parse("O=PartyB,L=New York,C=US"))

    //eigentlich richtig, aber test läuft nicht
    private var linearId: UniqueIdentifier = UniqueIdentifier()

    init {
        listOf(questionerNode, respondentNode).forEach {
            it.registerInitiatedFlow(AskFlowResponder::class.java)
            it.registerInitiatedFlow(AnswerFlowResponder::class.java)
        }
    }

    @Before
    fun setup() {
        network.runNetwork()

        // val linearIdentifier = linearId

        // In this setup function (It runs before the test function bellow), we will make a question and store its
        // external id
        val respondentParty = respondentNode.info.chooseIdentityAndCert().party
        val flow = AskFlow("My question", respondentParty)
        val future = questionerNode.startFlow(flow)
        network.runNetwork()
        val ptx: SignedTransaction = future.getOrThrow()
        // Print the transaction for debugging purposes.
        println(ptx.tx)

        // We store the external Id, we want to use to make an answer
        linearId = ptx.tx.outputsOfType(QAState::class.java).first().linearId
    }


    @After
    fun tearDown() = network.stopNodes()

    @Test
    fun testAnswer() {

        // linear ID statt externalId
        val flow = AnswerFlow("My answer", linearId)
        val future = respondentNode.startFlow(flow)
        network.runNetwork()
        val ptx: SignedTransaction = future.getOrThrow()
        // Print the transaction for debugging purposes.
        println(ptx.tx)
    }
}

