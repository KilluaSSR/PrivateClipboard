package killua.dev.privateclipboard.data

import android.content.Context
import android.view.inputmethod.InputMethodManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InputMethods @Inject constructor(@ApplicationContext private val context: Context) {

    /** Packages of the input methods the current user has enabled. */
    fun enabledPackages(): Set<String> = runCatching {
        context.getSystemService(InputMethodManager::class.java)
            .enabledInputMethodList
            .mapTo(mutableSetOf()) { it.packageName }
    }.getOrDefault(emptySet())
}
