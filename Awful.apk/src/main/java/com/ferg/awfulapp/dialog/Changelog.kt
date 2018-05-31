package com.ferg.awfulapp.dialog

import android.content.Context
import android.support.v7.app.AlertDialog
import android.text.Html
import com.ferg.awfulapp.R
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException

/**
 * Created by baka kaba on 23/03/2016.
 *
 * Displays the current changelog.
 */
object Changelog {

    /**
     * Display the current changelog as a dialog, optionally limiting it to the most recent [maxEntries]
     */
    @JvmStatic
    fun showDialog(context: Context, maxEntries: Int?) {
        val changelogText = try {
            context.assets.open("changelog.html").use { inStream ->
                with(Jsoup.parse(inStream, null, "")) {
                    maxEntries?.let { stripEntries(maxEntries) }
                    outerHtml()
                }
            }
        } catch (e: IOException) {
            "Couldn't read changelog!"
        }

        // Build a basic dialog with the changelog html
        AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.changelog_dialog_title))
                .setMessage(Html.fromHtml(changelogText))
                .setPositiveButton(context.getString(R.string.alert_ok)) { dialog, _ -> dialog.dismiss() }.show()

    }


    /**
     * Remove changelog entry elements, leaving only the most recent [maxCount] entries.
     */
    private fun Document.stripEntries(maxCount: Int) {
        /*
        this keeps the first n elements under <main> and deletes the rest, so it expects the entries to be the *only* direct children, i.e.
        <main>
            <section> entry 1...</section>
            <section> entry 2...</section>
        </main>
        so don't add anything between sections! Put it inside a <section> block if you really need to
        */
        val maxIndex = (maxCount - 1).coerceAtLeast(0)
        select("main > :gt($maxIndex)").remove()
    }

}
