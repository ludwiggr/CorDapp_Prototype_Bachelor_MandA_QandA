package com.template.states
//com.template.states
import com.template.contracts.QAContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

/** COMMENT */
// Comment

/**
 * The State is a LinearState, this means it's a state that can evolve, it has a unique identifier that tracks it.
 * i.e QAState can evolve over time, we can for example updating the answer with a new one, the the state with the old
 * answer will be archived (We can see it but no longer use it in a transaction, for auditing reasons), and the new
 * state will be saved.
 */

/**
 * Constructor: It has the fields that we need to create a new QA state.
 * I have added an externalId, it's used to construct the UniqueIdentifier, why? because when the respondent answers
 * , he must retrieve the state from the vault (database) to put his answer in it, he retrieves it by providing the externalId
 * which must be unique. I think it's the same as "questionNumber" u put before.
 */
// Create new State for Contracts to run on
@BelongsToContract(QAContract::class)
data class QAState(
        //define which type parties are
        val question: String,
        val questioner: Party,
        val respondent: Party,
        //Using var instead of val so we can reassign the answer later on
        //default answer value set to null
        var answer: String? = null,
//        //geld zum blocking mit answer
//        var money: Int? = null,
        //A random new instance of UniqueIdentifier is generated with every new state created
        override val linearId: UniqueIdentifier = UniqueIdentifier()
//Set participants - required by corda
) : LinearState {
    override val participants: List<AbstractParty> = listOf(questioner, respondent)
}

/* usage of externalId possible instead of linearId, if state should be linked
to an external Question Number or something like it...
* */
