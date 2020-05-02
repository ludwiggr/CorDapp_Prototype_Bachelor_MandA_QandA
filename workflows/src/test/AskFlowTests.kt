package com.template//package com.template

import com.template.flows.AskFlow

import com.template.flows.AskFlowResponder
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


class AskFlowTests {
    private val network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
            TestCordapp.findCordapp("com.template.contracts"),
            TestCordapp.findCordapp("com.template.flows")
    )))
    private val questionerNode = network.createNode()
    private val respondentNode = network.createNode(CordaX500Name.parse("O=PartyB,L=New York,C=US"))

    init {
        listOf(questionerNode, respondentNode).forEach {
            it.registerInitiatedFlow(AskFlowResponder::class.java)
        }
    }

    @Before
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()

    @Test
    fun `dummy test`() {

//        val questionerParty = questionerNode.info.chooseIdentityAndCert().party
        val repondentParty = respondentNode.info.chooseIdentityAndCert().party

        val flow = AskFlow("My question", repondentParty)
        val future = questionerNode.startFlow(flow)
        network.runNetwork()
        val ptx: SignedTransaction = future.getOrThrow()
        // Print the transaction for debugging purposes.
        println(ptx.tx)
    }

}