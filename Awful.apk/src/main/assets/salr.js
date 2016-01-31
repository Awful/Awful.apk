// Copyright (c) 2009-2013 Scott Ferguson
// Copyright (c) 2013-2014 Matthew Peveler
// All rights reserved.

// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:

// - Redistributions of source code must retain the above copyright
//   notice, this list of conditions and the following disclaimer.
// - Redistributions in binary form must reproduce the above copyright
//   notice, this list of conditions and the following disclaimer in the
//   documentation and/or other materials provided with the distribution.
// - Neither the name of the software nor the
//   names of its contributors may be used to endorse or promote products
//   derived from this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHORS ''AS IS'' AND ANY
// EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY
// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

function SALR(javascriptinterface) {
    this.javascriptinterface = javascriptinterface;

    this.init();
};

SALR.prototype.init = function() {
    if (this.javascriptinterface.getPreference("highlightUsername") == "true") {
        this.highlightOwnUsername();
    }

    if (this.javascriptinterface.getPreference("highlightUserQuote") == "true") {
        this.highlightOwnQuotes();
    }

    if (this.javascriptinterface.getPreference("inlineTweets") == "true") {
        this.inlineTweets();
    }

    if (this.javascriptinterface.getPreference("inlineWebm") == "true") {
        this.inlineWebm();
    }

    if (this.javascriptinterface.getPreference("inlineVines") == "true") {
        this.inlineVines();
    }

}
/**
 * Highlight the user's username in posts
 */
SALR.prototype.highlightOwnUsername = function() {
    function getTextNodesIn(node) {
        var textNodes = [];

        function getTextNodes(node) {
            if (node.nodeType == 3) {
                textNodes.push(node);
            } else {
                for (var i = 0, len = node.childNodes.length; i < len; ++i) {
                    getTextNodes(node.childNodes[i]);
                }
            }
        }

        getTextNodes(node);
        return textNodes;
    }

    var that = this;

    var selector = '.postcontent:contains("' + that.javascriptinterface.getPreference("username") + '")';
    
    var re = new RegExp('\\b'+that.javascriptinterface.getPreference("username")+'\\b', 'g');
    var styled = '<span class="usernameHighlight">' + that.javascriptinterface.getPreference("username") + '</span>';
    $(selector).each(function() {
        getTextNodesIn(this).forEach(function(node) {
            if(node.wholeText.match(re)) {
                newNode = node.ownerDocument.createElement("span");
                $(newNode).html(node.wholeText.replace(re, '<span class="usernameHighlight">' + that.javascriptinterface.getPreference("username") + '</span>'));
                node.parentNode.replaceChild(newNode, node);
            }
        });
    });
};

/**
 * Highlight the quotes of the user themselves.
 */
SALR.prototype.highlightOwnQuotes = function() {
    var that = this;

    var usernameQuoteMatch = that.javascriptinterface.getPreference("username") + ' posted:';
    $('.bbc-block h4:contains(' + usernameQuoteMatch + ')').each(function() {
        if ($(this).text() != usernameQuoteMatch)
            return;
        $(this).parent().addClass("self");
        // Replace the styling from username highlighting
        var previous = $(this);
        $('.usernameHighlight', previous).each(function() {
            $(this).removeClass('usernameHighlight');
        });
    });
};
SALR.prototype.inlineTweets = function() {

    var that = this;
    var tweets = $('.postcontent a[href*="twitter.com"]');
    //NWS/NMS links

    //tweets = tweets.not(".postcontent:has(img[title=':nws:']) a").not(".postcontent:has(img[title=':nms:']) a");

    // spoiler'd links
    tweets = tweets.not('.bbc-spoiler a');
    tweets.each(function() {
        var match = $(this).attr('href').match(/(https|http):\/\/twitter.com\/[0-9a-zA-Z_]+\/(status|statuses)\/([0-9]+)/);
        if (match == null) {
            return;
        }
        var tweetId = match[3];
        var link = $(this);
        $.ajax({url:"https://api.twitter.com/1/statuses/oembed.json?id="+tweetId,
            dataType: 'jsonp',
            success: function(data) {
                link = $(link).wrap("<div class='tweet'>").parent();
                datahtml = data.html.replace("src=\"//platform.twitter.com/widgets.js\"", "src=\"file:///android_asset/twitterwidget.js\"");
                $(link).html(datahtml);
                if($('head').children('link').first().attr('href').indexOf('dark.css') != -1 || $('head').children('link').first().attr('href').indexOf('pos.css') != -1){
                    $(link).children('blockquote').first().data('theme','dark');
                }
                window.twttr.widgets.load();
            }
        });
    });
};

SALR.prototype.inlineVines = function() {
    var that = this;
    var vines = $('.postcontent a[href*="://vine.co/v/"]');

    //vines = vines.not(".postcontent:has(img[title=':nws:']) a").not(".postcontent:has(img[title=':nms:']) a");

    // spoiler'd links
    vines = vines.not('.bbc-spoiler a');
    vines.each(function() {
        $(this).html('<iframe class="vine-embed" src="'+$(this).attr('href')+'/embed/simple" width="600" height="600" frameborder="0"></iframe>'+
                          '<script async src="http://platform.vine.co/static/scripts/embed.js" charset="utf-8"></script>');
    });
};

SALR.prototype.inlineWebm = function() {
    var that = this;
    var webms = $('.postcontent a[href$="webm"],.postcontent a[href$="gifv"],.postcontent a[href$="mp4"]');

    //webms = webms.not(".postcontent:has(img[title=':nws:']) a").not(".postcontent:has(img[title=':nms:']) a");

    // spoiler'd links
    webms = webms.not('.bbc-spoiler a');

    webms.each(function() {
        $(this).replaceWith('<video loop width="100%" muted="true" controls preload="metadata"> <source src="'+$(this).attr('href').substr(0, $(this).attr('href').lastIndexOf('.'))+'.webm" type="video/webm"> </video>');
    });
};