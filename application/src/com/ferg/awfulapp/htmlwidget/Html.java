/*-
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications:
 * - Don't add placeholder image if ImageGetter returns null
 * - Call TagHandler with <img> tag when ImageGetter is null
 * - Added Attributes parameter to TagHandler
 * - Changed access level to package-protected
 * - Changed package name
 */

package com.ferg.awfulapp.htmlwidget;

import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.htmlcleaner.DomSerializer;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.TagNodeVisitor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.ferg.awfulapp.R;
import com.ferg.awfulapp.preferences.AwfulPreferences;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.AlignmentSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.ParagraphStyle;
import android.text.style.QuoteSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.TypefaceSpan;
import android.text.style.URLSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;

/**
 * This class processes HTML strings into displayable styled text.
 * Not all HTML tags are supported.
 */
class Html {
    /**
     * Retrieves images for HTML &lt;img&gt; tags.
     */
    public static interface ImageGetter {
        /**
         * This methos is called when the HTML parser encounters an
         * &lt;img&gt; tag.  The <code>source</code> argument is the
         * string from the "src" attribute; the return value should be
         * a Drawable representation of the image or <code>null</code>
         * for a generic replacement image.  Make sure you call
         * setBounds() on your Drawable if it doesn't already have
         * its bounds set.
         */
        public Drawable getDrawable(String source);
    }

    /**
     * Is notified when HTML tags are encountered that the parser does
     * not know how to interpret.
     */
    public static interface TagHandler {
        /**
         * This gets called when a tag opening is encountered. This method should just put the
         * required Spannable in the output parameter - recursively handling the TagNode
         * contents is NOT the responsibility of this handler
         */
        public void handleStartTag(Element node, Editable output);
        
        /**
         * This gets called when a closing tag is encountered. This method should just put the
         * required Spannable in the output parameter - recursively handling the TagNode
         * contents is NOT the responsibility of this handler
         */
        public void handleEndTag(Element node, Editable output);
    }

    private Html() { }

    /**
     * Returns displayable styled text from the provided HTML string.
     * Any &lt;img&gt; tags in the HTML will display as a generic
     * replacement image which your program can then go through and
     * replace with real images.
     *
     * <p>This uses TagSoup to handle real HTML, including all of the brokenness found in the wild.
     */
    public static Spanned fromHtml(String source) {
        return fromHtml(source, null, null, null);
    }

    /**
     * Returns displayable styled text from the provided HTML string.
     * Any &lt;img&gt; tags in the HTML will use the specified ImageGetter
     * to request a representation of the image (use null if you don't
     * want this) and the specified TagHandler to handle unknown tags
     * (specify null if you don't want this).
     *
     * <p>This uses TagSoup to handle real HTML, including all of the brokenness found in the wild.
     */
    public static Spanned fromHtml(String source, ImageGetter imageGetter,
                                   TagHandler tagHandler, Context context) {
        HtmlCleaner cleaner = new HtmlCleaner();
        
        HtmlToSpannedConverter converter =
                new HtmlToSpannedConverter(source, imageGetter, tagHandler,
                        cleaner,context);
        return converter.convert();
    }

    /**
     * Returns an HTML representation of the provided Spanned text.
     */
    public static String toHtml(Spanned text) {
        StringBuilder out = new StringBuilder();
        withinHtml(out, text);
        return out.toString();
    }

    private static void withinHtml(StringBuilder out, Spanned text) {
        int len = text.length();

        int next;
        for (int i = 0; i < text.length(); i = next) {
            next = text.nextSpanTransition(i, len, ParagraphStyle.class);
            ParagraphStyle[] style = text.getSpans(i, next, ParagraphStyle.class);
            String elements = " ";
            boolean needDiv = false;

            for(int j = 0; j < style.length; j++) {
                if (style[j] instanceof AlignmentSpan) {
                    Layout.Alignment align =
                        ((AlignmentSpan) style[j]).getAlignment();
                    needDiv = true;
                    if (align == Layout.Alignment.ALIGN_CENTER) {
                        elements = "align=\"center\" " + elements;
                    } else if (align == Layout.Alignment.ALIGN_OPPOSITE) {
                        elements = "align=\"right\" " + elements;
                    } else {
                        elements = "align=\"left\" " + elements;
                    }
                }
            }
            if (needDiv) {
                out.append("<div " + elements + ">");
            }

            withinDiv(out, text, i, next);

            if (needDiv) {
                out.append("</div>");
            }
        }
    }

    private static void withinDiv(StringBuilder out, Spanned text,
            int start, int end) {
        int next;
        for (int i = start; i < end; i = next) {
            next = text.nextSpanTransition(i, end, QuoteSpan.class);
            QuoteSpan[] quotes = text.getSpans(i, next, QuoteSpan.class);

            for (int j=0;j<quotes.length;j++) {
                out.append("<blockquote>");
            }

            withinBlockquote(out, text, i, next);

            for (int j=0;j<quotes.length;j++) {
                out.append("</blockquote>\n");
            }
        }
    }

    private static void withinBlockquote(StringBuilder out, Spanned text,
                                         int start, int end) {
        out.append("<p>");

        int next;
        for (int i = start; i < end; i = next) {
            next = TextUtils.indexOf(text, '\n', i, end);
            if (next < 0) {
                next = end;
            }

            int nl = 0;

            while (next < end && text.charAt(next) == '\n') {
                nl++;
                next++;
            }

            withinParagraph(out, text, i, next - nl, nl, next == end);
        }

        out.append("</p>\n");
    }

    private static void withinParagraph(StringBuilder out, Spanned text,
                                        int start, int end, int nl,
                                        boolean last) {
        int next;
        for (int i = start; i < end; i = next) {
            next = text.nextSpanTransition(i, end, CharacterStyle.class);
            CharacterStyle[] style = text.getSpans(i, next,
                                                   CharacterStyle.class);

            for (int j = 0; j < style.length; j++) {
                if (style[j] instanceof StyleSpan) {
                    int s = ((StyleSpan) style[j]).getStyle();

                    if ((s & Typeface.BOLD) != 0) {
                        out.append("<b>");
                    }
                    if ((s & Typeface.ITALIC) != 0) {
                        out.append("<i>");
                    }
                }
                if (style[j] instanceof TypefaceSpan) {
                    String s = ((TypefaceSpan) style[j]).getFamily();

                    if (s.equals("monospace")) {
                        out.append("<tt>");
                    }
                }
                if (style[j] instanceof SuperscriptSpan) {
                    out.append("<sup>");
                }
                if (style[j] instanceof SubscriptSpan) {
                    out.append("<sub>");
                }
                if (style[j] instanceof UnderlineSpan) {
                    out.append("<u>");
                }
                if (style[j] instanceof StrikethroughSpan) {
                    out.append("<strike>");
                }
                if (style[j] instanceof URLSpan) {
                    out.append("<a href=\"");
                    out.append(((URLSpan) style[j]).getURL());
                    out.append("\">");
                }
                if (style[j] instanceof ImageSpan) {
                    out.append("<img src=\"");
                    out.append(((ImageSpan) style[j]).getSource());
                    out.append("\">");

                    // Don't output the dummy character underlying the image.
                    i = next;
                }
                if (style[j] instanceof AbsoluteSizeSpan) {
                    out.append("<font size =\"");
                    out.append(((AbsoluteSizeSpan) style[j]).getSize() / 6);
                    out.append("\">");
                }
                if (style[j] instanceof ForegroundColorSpan) {
                    out.append("<font color =\"#");
                    String color = Integer.toHexString(((ForegroundColorSpan)
                            style[j]).getForegroundColor() + 0x01000000);
                    while (color.length() < 6) {
                        color = "0" + color;
                    }
                    out.append(color);
                    out.append("\">");
                }
            }

            withinStyle(out, text, i, next);

            for (int j = style.length - 1; j >= 0; j--) {
                if (style[j] instanceof ForegroundColorSpan) {
                    out.append("</font>");
                }
                if (style[j] instanceof AbsoluteSizeSpan) {
                    out.append("</font>");
                }
                if (style[j] instanceof URLSpan) {
                    out.append("</a>");
                }
                if (style[j] instanceof StrikethroughSpan) {
                    out.append("</strike>");
                }
                if (style[j] instanceof UnderlineSpan) {
                    out.append("</u>");
                }
                if (style[j] instanceof SubscriptSpan) {
                    out.append("</sub>");
                }
                if (style[j] instanceof SuperscriptSpan) {
                    out.append("</sup>");
                }
                if (style[j] instanceof TypefaceSpan) {
                    String s = ((TypefaceSpan) style[j]).getFamily();

                    if (s.equals("monospace")) {
                        out.append("</tt>");
                    }
                }
                if (style[j] instanceof StyleSpan) {
                    int s = ((StyleSpan) style[j]).getStyle();

                    if ((s & Typeface.BOLD) != 0) {
                        out.append("</b>");
                    }
                    if ((s & Typeface.ITALIC) != 0) {
                        out.append("</i>");
                    }
                }
            }
        }

        String p = last ? "" : "</p>\n<p>";

        if (nl == 1) {
            out.append("<br>\n");
        } else if (nl == 2) {
            out.append(p);
        } else {
            for (int i = 2; i < nl; i++) {
                out.append("<br>");
            }

            out.append(p);
        }
    }

    private static void withinStyle(StringBuilder out, Spanned text,
                                    int start, int end) {
        for (int i = start; i < end; i++) {
            char c = text.charAt(i);

            if (c == '<') {
                out.append("&lt;");
            } else if (c == '>') {
                out.append("&gt;");
            } else if (c == '&') {
                out.append("&amp;");
            } else if (c > 0x7E || c < ' ') {
                out.append("&#" + ((int) c) + ";");
            } else if (c == ' ') {
                while (i + 1 < end && text.charAt(i + 1) == ' ') {
                    out.append("&nbsp;");
                    i++;
                }

                out.append(' ');
            } else {
                out.append(c);
            }
        }
    }
}

class HtmlToSpannedConverter {

    private static final float[] HEADER_SIZES = {
        1.5f, 1.4f, 1.3f, 1.2f, 1.1f, 1f,
    };

    private String mSource;
    private HtmlCleaner mCleaner;
    private SpannableStringBuilder mSpannableStringBuilder;
    private Html.ImageGetter mImageGetter;
    private Html.TagHandler mTagHandler;
    private DomSerializer mDomSerializer;
    private Context mContext;

    public HtmlToSpannedConverter(
            String source, Html.ImageGetter imageGetter, Html.TagHandler tagHandler,
            HtmlCleaner cleaner, Context context) {
        mSource = source;
	   mContext = context;
        mSpannableStringBuilder = new SpannableStringBuilder();
        mImageGetter = imageGetter;
        mTagHandler = tagHandler;
        
        mCleaner = cleaner;
        mDomSerializer = new DomSerializer(cleaner.getProperties());
    }

    public Spanned convert() {
    	TagNode rootTagNode = mCleaner.clean(mSource);
    	Document root;
		try {
			root = mDomSerializer.createDOM(rootTagNode);
		} catch (ParserConfigurationException e) {
			// Shouldn't happen, so long as HtmlCleaner is valid
			e.printStackTrace();
			return null;
		}
    	traverse(root.getDocumentElement());
    	
        // Fix flags and range for paragraph-type markup.
        Object[] obj = mSpannableStringBuilder.getSpans(0, mSpannableStringBuilder.length(), ParagraphStyle.class);
        for (int i = 0; i < obj.length; i++) {
            int start = mSpannableStringBuilder.getSpanStart(obj[i]);
            int end = mSpannableStringBuilder.getSpanEnd(obj[i]);

            // If the last line of the range is blank, back off by one.
            if (end - 2 >= 0) {
                if (mSpannableStringBuilder.charAt(end - 1) == '\n' &&
                    mSpannableStringBuilder.charAt(end - 2) == '\n') {
                    end--;
                }
            }

            if (end == start) {
                mSpannableStringBuilder.removeSpan(obj[i]);
            } else {
                mSpannableStringBuilder.setSpan(obj[i], start, end, Spannable.SPAN_PARAGRAPH);
            }
        }

        return mSpannableStringBuilder;
    }

    private void traverse(Element node) {
		handleStartTag(node);
		
		NodeList children = node.getChildNodes();
		int numChildren = children.getLength();
		Node child;
		for(int i=0;i<numChildren;i++) {
			child = children.item(i);
			switch(child.getNodeType()) {
			case Node.ELEMENT_NODE:
				traverse((Element) child);
				break;
			case Node.TEXT_NODE:
				characters(((Text) child).getData());
				break;
			default:
				Log.w("Html", "Traversing non-text, non-element node");
			}
		}
		
		handleEndTag(node);
	}
	

    public void characters(String str) {
    	char[] ch = str.toCharArray();
    	int length = ch.length;
    	
        StringBuilder sb = new StringBuilder();

        /*
         * Ignore whitespace that immediately follows other whitespace;
         * newlines count as spaces.
         */

        for (int i = 0; i < length; i++) {
            char c = ch[i];

            // If we're in something like a code block, we do actually want line breaks
            // to break lines
            if ((mNumTagsEnforcingTrueWhitespace == 0) && (c == ' ' || c == '\n')) {
                char pred;
                int len = sb.length();

                if (len == 0) {
                    len = mSpannableStringBuilder.length();

                    if (len == 0) {
                        pred = '\n';
                    } else {
                        pred = mSpannableStringBuilder.charAt(len - 1);
                    }
                } else {
                    pred = sb.charAt(len - 1);
                }

                if (pred != ' ' && pred != '\n') {
                    sb.append(' ');
                }
            } else {
                sb.append(c);
            }
        }

        mSpannableStringBuilder.append(sb);
    }
    
    private int mNumTagsEnforcingTrueWhitespace = 0;
    
    private void handleStartTag(Element node) {
    	String tag = node.getTagName();
    	
        if (tag.equalsIgnoreCase("br")) {
            // We don't need to handle this. TagSoup will ensure that there's a </br> for each <br>
            // so we can safely emit the linebreaks when we handle the close tag.
        } else if (tag.equalsIgnoreCase("p") && "editedby".equals(node.getAttribute("class"))) {
        	handleP(mSpannableStringBuilder);
            start(mSpannableStringBuilder, new Italic());
            start(mSpannableStringBuilder, new Small());
        } else if (tag.equalsIgnoreCase("p")) {
            handleP(mSpannableStringBuilder);
        } else if (tag.equalsIgnoreCase("div")) {
            handleP(mSpannableStringBuilder);
        } else if (tag.equalsIgnoreCase("em")) {
            start(mSpannableStringBuilder, new Bold());
        } else if (tag.equalsIgnoreCase("b")) {
            start(mSpannableStringBuilder, new Bold());
        } else if (tag.equalsIgnoreCase("strong")) {
            start(mSpannableStringBuilder, new Italic());
        } else if (tag.equalsIgnoreCase("cite")) {
            start(mSpannableStringBuilder, new Italic());
        } else if (tag.equalsIgnoreCase("dfn")) {
            start(mSpannableStringBuilder, new Italic());
        } else if (tag.equalsIgnoreCase("i")) {
            start(mSpannableStringBuilder, new Italic());
        } else if (tag.equalsIgnoreCase("big")) {
            start(mSpannableStringBuilder, new Big());
        } else if (tag.equalsIgnoreCase("small")) {
            start(mSpannableStringBuilder, new Small());
        } else if (tag.equalsIgnoreCase("font")) {
            startFont(mSpannableStringBuilder, node);
        } else if (tag.equalsIgnoreCase("blockquote")) {
            handleP(mSpannableStringBuilder);
            start(mSpannableStringBuilder, new Blockquote());
        } else if (tag.equalsIgnoreCase("tt")) {
            start(mSpannableStringBuilder, new Monospace());
        } else if (tag.equalsIgnoreCase("pre")) {
            start(mSpannableStringBuilder, new Monospace());
        } else if (tag.equalsIgnoreCase("code")) {
        	mNumTagsEnforcingTrueWhitespace++;
        } else if (tag.equalsIgnoreCase("a")) {
            startA(mSpannableStringBuilder, node);
        } else if (tag.equalsIgnoreCase("u")) {
            start(mSpannableStringBuilder, new Underline());
        } else if (tag.equalsIgnoreCase("sup")) {
            start(mSpannableStringBuilder, new Super());
        } else if (tag.equalsIgnoreCase("sub")) {
            start(mSpannableStringBuilder, new Sub());
        } else if (tag.length() == 2 &&
                   Character.toLowerCase(tag.charAt(0)) == 'h' &&
                   tag.charAt(1) >= '1' && tag.charAt(1) <= '6') {
            handleP(mSpannableStringBuilder);
            start(mSpannableStringBuilder, new Header(tag.charAt(1) - '1'));
        } else if (tag.equalsIgnoreCase("img") && mImageGetter != null) {
            startImg(mSpannableStringBuilder, node, mImageGetter);
        } else if(tag.equalsIgnoreCase("span")){
        	if(node.getAttribute("class").equals("bbc-spoiler")){
        		start(mSpannableStringBuilder, new Span());
        	}
        } else if (mTagHandler != null) {
            mTagHandler.handleStartTag(node, mSpannableStringBuilder);
        }
    }

    private void handleEndTag(Element node) {
    	String tag = node.getTagName();
    	
        if (tag.equalsIgnoreCase("br")) {
            handleBr(mSpannableStringBuilder);
        } else if (tag.equalsIgnoreCase("p") && "editedby".equals(node.getAttribute("class"))) {
        	handleP(mSpannableStringBuilder);
            end(mSpannableStringBuilder, Italic.class, new StyleSpan(Typeface.ITALIC));
            end(mSpannableStringBuilder, Small.class, new RelativeSizeSpan(0.8f));
        } else if (tag.equalsIgnoreCase("p")) {
            handleP(mSpannableStringBuilder);
        } else if (tag.equalsIgnoreCase("div")) {
            handleP(mSpannableStringBuilder);
        } else if (tag.equalsIgnoreCase("em")) {
            end(mSpannableStringBuilder, Bold.class, new StyleSpan(Typeface.BOLD));
        } else if (tag.equalsIgnoreCase("b")) {
            end(mSpannableStringBuilder, Bold.class, new StyleSpan(Typeface.BOLD));
        } else if (tag.equalsIgnoreCase("strong")) {
            end(mSpannableStringBuilder, Italic.class, new StyleSpan(Typeface.ITALIC));
        } else if (tag.equalsIgnoreCase("cite")) {
            end(mSpannableStringBuilder, Italic.class, new StyleSpan(Typeface.ITALIC));
        } else if (tag.equalsIgnoreCase("dfn")) {
            end(mSpannableStringBuilder, Italic.class, new StyleSpan(Typeface.ITALIC));
        } else if (tag.equalsIgnoreCase("i")) {
            end(mSpannableStringBuilder, Italic.class, new StyleSpan(Typeface.ITALIC));
        } else if (tag.equalsIgnoreCase("big")) {
            end(mSpannableStringBuilder, Big.class, new RelativeSizeSpan(1.25f));
        } else if (tag.equalsIgnoreCase("small")) {
            end(mSpannableStringBuilder, Small.class, new RelativeSizeSpan(0.8f));
        } else if (tag.equalsIgnoreCase("font")) {
            endFont(mSpannableStringBuilder);
        } else if (tag.equalsIgnoreCase("blockquote")) {
            handleP(mSpannableStringBuilder);
            end(mSpannableStringBuilder, Blockquote.class, new QuoteSpan(PreferenceManager.getDefaultSharedPreferences(mContext).getInt("link_quote_color", mContext.getResources().getColor(R.color.dark_link_quote))));
        } else if (tag.equalsIgnoreCase("tt")) {
            end(mSpannableStringBuilder, Monospace.class, new TypefaceSpan("monospace"));
        } else if (tag.equalsIgnoreCase("pre")) {
        	end(mSpannableStringBuilder, Monospace.class, new TypefaceSpan("monospace"));
        } else if (tag.equalsIgnoreCase("code")) {
        	mNumTagsEnforcingTrueWhitespace--;
        } else if (tag.equalsIgnoreCase("a")) {
            endA(mSpannableStringBuilder,mContext);
        } else if (tag.equalsIgnoreCase("u")) {
            end(mSpannableStringBuilder, Underline.class, new UnderlineSpan());
        } else if (tag.equalsIgnoreCase("sup")) {
            end(mSpannableStringBuilder, Super.class, new SuperscriptSpan());
        } else if (tag.equalsIgnoreCase("sub")) {
            end(mSpannableStringBuilder, Sub.class, new SubscriptSpan());
        }else if (tag.equalsIgnoreCase("span")) {
        	if(node.getAttribute("class").equals("bbc-spoiler")){
            endSpan(mSpannableStringBuilder,mContext);
        	}
        } else if (tag.length() == 2 &&
                Character.toLowerCase(tag.charAt(0)) == 'h' &&
                tag.charAt(1) >= '1' && tag.charAt(1) <= '6') {
            handleP(mSpannableStringBuilder);
            endHeader(mSpannableStringBuilder);
        } else if (mTagHandler != null) {
            mTagHandler.handleEndTag(node, mSpannableStringBuilder);
        }
    }

    public static final int
    convertValueToInt(CharSequence charSeq, int defaultValue)
    {
        if (null == charSeq)
            return defaultValue;

        String nm = charSeq.toString();

        // This code is copied from Integer.decode() so we don't
        // have to instantiate an Integer!

        int sign = 1;
        int index = 0;
        int len = nm.length();
        int base = 10;

        if ('-' == nm.charAt(0)) {
            sign = -1;
            index++;
        }

        if ('0' == nm.charAt(index)) {
            //  Quick check for a zero by itself
            if (index == (len - 1))
                return 0;

            char    c = nm.charAt(index + 1);

            if ('x' == c || 'X' == c) {
                index += 2;
                base = 16;
            } else {
                index++;
                base = 8;
            }
        }
        else if ('#' == nm.charAt(index))
        {
            index++;
            base = 16;
        }

        return Integer.parseInt(nm.substring(index), base) * sign;
    }


    private static void handleP(SpannableStringBuilder text) {
        int len = text.length();

        if (len >= 1 && text.charAt(len - 1) == '\n') {
            if (len >= 2 && text.charAt(len - 2) == '\n') {
                return;
            }

            text.append("\n");
            return;
        }

        if (len != 0) {
            text.append("\n\n");
        }
    }

    private static void handleBr(SpannableStringBuilder text) {
        text.append("\n");
    }

    private static <T> T getLast(Spanned text, Class<T> kind) {
        /*
         * This knows that the last returned object from getSpans()
         * will be the most recently added.
         */
        T[] objs = text.getSpans(0, text.length(), kind);

        if (objs.length == 0) {
            return null;
        } else {
            return objs[objs.length - 1];
        }
    }

    private static void start(SpannableStringBuilder text, Object mark) {
        int len = text.length();
        text.setSpan(mark, len, len, Spannable.SPAN_MARK_MARK);
    }

    private static <T> void end(SpannableStringBuilder text, Class<T> kind,
                            Object repl) {
        int len = text.length();
        Object obj = getLast(text, kind);
        int where = text.getSpanStart(obj);

        text.removeSpan(obj);

        if (where != len) {
            text.setSpan(repl, where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return;
    }

    private static void startImg(SpannableStringBuilder text, Element node, Html.ImageGetter img) {
        String src = node.getAttribute("src");
        Drawable d = null;

        if (img != null) {
            d = img.getDrawable(src);
        }

        if (d == null) {
            return;
        }

        int len = text.length();
        text.append("\uFFFC");

        text.setSpan(new ImageSpan(d, src), len, text.length(),
                     Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private static void startFont(SpannableStringBuilder text, Element node) {
        String color = node.getAttribute("color");
        String face = node.getAttribute("face");

        int len = text.length();
        text.setSpan(new Font(color, face), len, len, Spannable.SPAN_MARK_MARK);
    }

    private static void endFont(SpannableStringBuilder text) {
        int len = text.length();
        Object obj = getLast(text, Font.class);
        int where = text.getSpanStart(obj);

        text.removeSpan(obj);

        if (where != len) {
            Font f = (Font) obj;

            if (f.mColor != null) {
                int c = -1;

                if (f.mColor.equalsIgnoreCase("aqua")) {
                    c = 0x00FFFF;
                } else if (f.mColor.equalsIgnoreCase("black")) {
                    c = 0x000000;
                } else if (f.mColor.equalsIgnoreCase("blue")) {
                    c = 0x0000FF;
                } else if (f.mColor.equalsIgnoreCase("fuchsia")) {
                    c = 0xFF00FF;
                } else if (f.mColor.equalsIgnoreCase("green")) {
                    c = 0x008000;
                } else if (f.mColor.equalsIgnoreCase("grey")) {
                    c = 0x808080;
                } else if (f.mColor.equalsIgnoreCase("lime")) {
                    c = 0x00FF00;
                } else if (f.mColor.equalsIgnoreCase("maroon")) {
                    c = 0x800000;
                } else if (f.mColor.equalsIgnoreCase("navy")) {
                    c = 0x000080;
                } else if (f.mColor.equalsIgnoreCase("olive")) {
                    c = 0x808000;
                } else if (f.mColor.equalsIgnoreCase("purple")) {
                    c = 0x800080;
                } else if (f.mColor.equalsIgnoreCase("red")) {
                    c = 0xFF0000;
                } else if (f.mColor.equalsIgnoreCase("silver")) {
                    c = 0xC0C0C0;
                } else if (f.mColor.equalsIgnoreCase("teal")) {
                    c = 0x008080;
                } else if (f.mColor.equalsIgnoreCase("white")) {
                    c = 0xFFFFFF;
                } else if (f.mColor.equalsIgnoreCase("yellow")) {
                    c = 0xFFFF00;
                } else {
                    try {
                        c = convertValueToInt(f.mColor, -1);
                    } catch (NumberFormatException nfe) {
                        // Can't understand the color, so just drop it.
                    }
                }

                if (c != -1) {
                    text.setSpan(new ForegroundColorSpan(c | 0xFF000000),
                                 where, len,
                                 Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }

            if (f.mFace != null) {
                text.setSpan(new TypefaceSpan(f.mFace), where, len,
                             Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    private static void startA(SpannableStringBuilder text, Element node) {
        String href = node.getAttribute("href");

        int len = text.length();
        text.setSpan(new Href(href), len, len, Spannable.SPAN_MARK_MARK);
    }

    private static void endA(SpannableStringBuilder text, Context context) {
        int len = text.length();
        Object obj = getLast(text, Href.class);
        int where = text.getSpanStart(obj);

        text.removeSpan(obj);

        if (where != len) {
            Href h = (Href) obj;

            if (h.mHref != null) {
                text.setSpan(new URLSpan(h.mHref), where, len,
                             Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                if(context != null){
                text.setSpan(new ForegroundColorSpan(PreferenceManager.getDefaultSharedPreferences(context).getInt("link_quote_color", context.getResources().getColor(R.color.link_quote))), where, len,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }

        }
    }

	private static void endSpan(SpannableStringBuilder text, Context context) {
        int len = text.length();
        Object obj = getLast(text, Span.class);
        int where = text.getSpanStart(obj);

        text.removeSpan(obj);
        if(where != len && where != -1){
        	int fg = PreferenceManager.getDefaultSharedPreferences(context).getInt("default_post_font_color",context.getResources().getColor(R.color.default_post_font));
        	int bg = PreferenceManager.getDefaultSharedPreferences(context).getInt("default_post_background_color", context.getResources().getColor(R.color.background));
            text.setSpan(new BackgroundColorSpan(fg), where, len, Spannable.SPAN_MARK_MARK);
            text.setSpan(new SpoilerSpan(fg,bg), where, len, Spannable.SPAN_MARK_MARK);

        }

    }

    private static void endHeader(SpannableStringBuilder text) {
        int len = text.length();
        Object obj = getLast(text, Header.class);

        int where = text.getSpanStart(obj);

        text.removeSpan(obj);

        // Back off not to change only the text, not the blank line.
        while (len > where && text.charAt(len - 1) == '\n') {
            len--;
        }

        if (where != len) {
            Header h = (Header) obj;

            text.setSpan(new RelativeSizeSpan(HEADER_SIZES[h.mLevel]),
                         where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            text.setSpan(new StyleSpan(Typeface.BOLD),
                         where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private static class Bold { }
    private static class Italic { }
    private static class Underline { }
    private static class Big { }
    private static class Small { }
    private static class Monospace { }
    private static class Blockquote { }
    private static class Super { }
    private static class Sub { }
    private static class Span { }

    private static class Font {
        public String mColor;
        public String mFace;

        public Font(String color, String face) {
            mColor = color;
            mFace = face;
        }
    }

    private static class Href {
        public String mHref;

        public Href(String href) {
            mHref = href;
        }
    }

    private static class Header {
        private int mLevel;

        public Header(int level) {
            mLevel = level;
        }
    }
}