package lt.pilietis.greitaspranesimas

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Nustatymai laikomi SharedPreferences, o ne BuildConfig — kad pasibaigus
 * žetono galiojimui nereikėtų iš naujo surinkinėti APK.
 */
class Prefs(context: Context) {
    private val p = context.getSharedPreferences("tvarkau", Context.MODE_PRIVATE)

    var userToken: String?
        get() = p.getString("user_token", null)?.ifBlank { null }
        set(v) = p.edit().putString("user_token", v).apply()

    var userId: Int
        get() = p.getInt("user_id", 0)
        set(v) = p.edit().putInt("user_id", v).apply()

    var reporterName: String?
        get() = p.getString("reporter_name", null)?.ifBlank { null }
        set(v) = p.edit().putString("reporter_name", v).apply()

    var reporterEmail: String?
        get() = p.getString("reporter_email", null)?.ifBlank { null }
        set(v) = p.edit().putString("reporter_email", v).apply()

    var reporterPhone: String?
        get() = p.getString("reporter_phone", null)?.ifBlank { null }
        set(v) = p.edit().putString("reporter_phone", v).apply()

    /** Sugeneruojamas kartą ir nebekeičiamas. */
    val serialNumber: String
        get() = p.getString("serial", null) ?: UUID.randomUUID().toString()
            .replace("-", "").take(12).uppercase()
            .also { p.edit().putString("serial", it).apply() }

    /** Užpildo TvarkauApi prieš bet kokį kreipimąsi. */
    fun applyTo(api: TvarkauApi = TvarkauApi) {
        api.userToken = userToken
        api.serialNumber = serialNumber
    }
}

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = Prefs(this)

        setContent {
            AppTheme {
                var token by remember { mutableStateOf(prefs.userToken.orEmpty()) }
                var userId by remember { mutableStateOf(prefs.userId.takeIf { it > 0 }?.toString().orEmpty()) }
                var name by remember { mutableStateOf(prefs.reporterName.orEmpty()) }
                var email by remember { mutableStateOf(prefs.reporterEmail.orEmpty()) }
                var phone by remember { mutableStateOf(prefs.reporterPhone.orEmpty()) }
                var status by remember { mutableStateOf<String?>(null) }
                var checking by remember { mutableStateOf(false) }

                fun save() {
                    prefs.userToken = token.trim()
                    prefs.userId = userId.trim().toIntOrNull() ?: 0
                    prefs.reporterName = name.trim()
                    prefs.reporterEmail = email.trim()
                    prefs.reporterPhone = phone.trim()
                }

                Column(
                    Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Prisijungimas", style = MaterialTheme.typography.headlineSmall)

                    Text(
                        "Žetoną paimk iš naršyklės: prisijunk tvarkaumiesta.lt, " +
                            "F12 → Application → Local Storage → ieškok user_token.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    OutlinedTextField(
                        value = token, onValueChange = { token = it },
                        label = { Text("user_token") },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = userId, onValueChange = { userId = it.filter(Char::isDigit) },
                        label = { Text("user_id (žetono tikrinimui)") },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { save(); status = "Išsaugota" },
                            modifier = Modifier.weight(1f)
                        ) { Text("Išsaugoti") }

                        OutlinedButton(
                            enabled = !checking && token.isNotBlank() && userId.isNotBlank(),
                            onClick = {
                                save(); checking = true; status = null
                                lifecycleScope.launch {
                                    prefs.applyTo()
                                    runCatching { TvarkauApi.tokenCheck(prefs.userId) }
                                        .onSuccess { status = "Galioja iki: $it" }
                                        .onFailure { status = "Klaida: ${it.message}" }
                                    checking = false
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text(if (checking) "Tikrinama…" else "Patikrinti") }
                    }

                    status?.let {
                        Text(it, maxLines = 4, overflow = TextOverflow.Ellipsis)
                    }

                    HorizontalDivider(Modifier.padding(vertical = 8.dp))

                    Text("Pranešėjo duomenys", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Šie laukai siunčiami ir be prisijungimo — gali pakakti " +
                            "atsakymui gauti.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        label = { Text("Vardas, pavardė") },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = email, onValueChange = { email = it },
                        label = { Text("El. paštas") },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone, onValueChange = { phone = it },
                        label = { Text("Telefonas") },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(Modifier.padding(vertical = 8.dp))

                    OutlinedButton(
                        enabled = !checking && prefs.userToken != null && prefs.userId > 0,
                        onClick = {
                            checking = true; status = null
                            lifecycleScope.launch {
                                prefs.applyTo()
                                runCatching {
                                    TvarkauApi.synchronize(prefs.userId, prefs.serialNumber)
                                }
                                    .onSuccess { status = "Ankstesni anoniminiai pranešimai susieti" }
                                    .onFailure { status = "Klaida: ${it.message}" }
                                checking = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Susieti anoniminius pranešimus su paskyra") }

                    Text(
                        "Šio įrenginio serial_number: ${prefs.serialNumber}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
