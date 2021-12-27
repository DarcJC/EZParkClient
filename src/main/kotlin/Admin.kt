import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Window
import io.ktor.client.request.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import pro.darc.park.utils.BASE_URL
import pro.darc.park.utils.httpClient
import java.util.*

@Serializable
data class ClientTokenCreateResponse(val uuid: String, val token: String)

@Composable
fun AdminApp() = Window(onCloseRequest = {}, title = "") {
    var token by remember { mutableStateOf("123456") }
    var result by remember { mutableStateOf("World") }

    MaterialTheme {
        Row {
            Column {
                TextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("Admin Token") }
                )
                TextField(value = result, onValueChange = {}, label = { Text("Result") })
            }
            Button(onClick = {
                GlobalScope.launch {
                    val res = httpClient.put<ClientTokenCreateResponse>("$BASE_URL/manage/client_token") {
                        parameter("token", token)
                    }
                    result = "uuid: ${res.uuid}, token: ${res.token}"
                }
            }) {
                Text("Create client credential")
            }
        }
    }
}
