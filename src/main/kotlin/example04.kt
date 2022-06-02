import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay

suspend fun main() {
    val user = User("roach")

    val suspendFunctionStatus = getUser(user)

    while (!suspendFunctionStatus.isCompleted) {
        println("Is Finished Async func ? ${suspendFunctionStatus.isCompleted}")
        println("User = ${user.name}")
        delay(1000)
    }
}

fun getUser(user: User) = CoroutineScope(Dispatchers.Default).async {
    println("Hi Async")
    user.name = "change"
}

data class User(
    var name: String
)
