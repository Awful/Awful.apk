//
// Created by baka kaba on 16/08/2017.
//
// Functions to automatically embed page content, e.g. turn an Instagram URL into a widget
//

function processThreadEmbeds() {

    // map preference keys to their corresponding embed functions
    var embedFunctions = {
        "inlineInstagram": embedInstagram
    }

    // check all embed preference keys - any set to true, run their embed function
    for (embedType in embedFunctions) {
        if (listener.getPreference(embedType) == "true") {
            embedFunctions[embedType].call()
        }
    }

    function embedInstagram() {
        // get each Instagram link, and replace it with the HTML from the embed API call
        var promises = []
        $('.postcontent').find('a[href*="instagr.am/p"], a[href*="instagram.com/p"]')
            .each(function() {
                var link = $(this)
                var url = link.attr('href')
                api_call = "https://api.instagram.com/oembed?omitscript=true&url=" + url + "&callback=?"
                promises.push($.getJSON(api_call, function(data) { link.replaceWith(data.html) }));
            });
        // do nothing if we have nothing to embed
        if (promises.length == 0) return

        // add the embed script, and run it after all the HTML widgets have been added
        $('<script>').attr('src', 'https://platform.instagram.com/en_US/embeds.js').appendTo('head')
        // potential race condition here if the promises complete before the script loads, so the function isn't available yet
        $.when.apply($, promises).then(function() { instgrm.Embeds.process() });
    }

}