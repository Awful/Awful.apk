function SALR(preferences) {
    this.preferences = preferences;

    this.init();
};

SALR.prototype.init = function() {
    this.highlightOwnQuotes();
};

/**
 * Highlight the quotes of the user themselves.
 */
SALR.prototype.highlightOwnQuotes = function() {
    var that = this;

    var usernameQuoteMatch = that.preferences.username + ' posted:';
    $('.bbc-block h4:contains(' + usernameQuoteMatch + ')').each(function() {
        if ($(this).text() != usernameQuoteMatch)
            return;
        $(this).parent().css("background-color", that.preferences.userQuote);
    });
};
