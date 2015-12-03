package com.itextpdf.core.font;

import com.itextpdf.basics.font.AdobeGlyphList;
import com.itextpdf.basics.font.FontConstants;
import com.itextpdf.basics.font.FontProgram;
import com.itextpdf.basics.font.PdfEncodings;
import com.itextpdf.basics.font.cmap.CMapLocation;
import com.itextpdf.basics.font.cmap.CMapLocationFromBytes;
import com.itextpdf.basics.font.cmap.CMapParser;
import com.itextpdf.basics.font.cmap.CMapToUnicode;
import com.itextpdf.core.pdf.PdfArray;
import com.itextpdf.core.pdf.PdfDictionary;
import com.itextpdf.core.pdf.PdfDocument;
import com.itextpdf.core.pdf.PdfName;
import com.itextpdf.core.pdf.PdfNumber;
import com.itextpdf.core.pdf.PdfObject;
import com.itextpdf.core.pdf.PdfStream;
import com.itextpdf.core.pdf.PdfString;

import java.io.IOException;
import java.util.Map;


public abstract class PdfSimpleFont<T extends FontProgram> extends PdfFont {

    protected T fontProgram;

    public PdfSimpleFont(PdfDocument document,PdfDictionary pdfDictionary) {
        super(document, pdfDictionary);
    }

    public PdfSimpleFont(PdfDocument document,PdfDictionary pdfDictionary, boolean isCopy) {
        super(document, pdfDictionary,isCopy);
    }

    @Override
    public T getFontProgram() {
        return fontProgram;
    }

    /**
     * Returns the width of a certain character of this font.
     *
     * @param ch a certain Unicode character.
     * @return a width in Text Space.
     */
    @Override
    public int getWidth(int ch) {
        int total = 0;
        byte[] bytes = fontProgram.getEncoding().convertToBytes(ch);
        for (byte b : bytes) {
            total += getFontProgram().getWidth(b & 0xff);
        }
        return total;
    }

    /**
     * Returns the width of a string of this font.
     *
     * @param s a Unicode string content.
     * @return a width of string in Text Space.
     */
    @Override
    public int getWidth(String s) {
        int total = 0;
        byte[] bytes = fontProgram.getEncoding().convertToBytes(s);
        for (byte b : bytes) {
            total += getFontProgram().getWidth(b & 0xff);
        }
        return total;
    }

    /**
     * Gets the descent of a {@code String} in normalized 1000 units. The descent will always be
     * less than or equal to zero even if all the characters have an higher descent.
     *
     * @param text the {@code String} to get the descent of
     * @return the descent in normalized 1000 units
     */
    public int getDescent(String text) {
        int min = 0;
        byte[] bytes = fontProgram.getEncoding().convertToBytes(text);
        for (byte b : bytes) {
            int[] bbox = getFontProgram().getCharBBox(b & 0xff);
            if (bbox != null && bbox[1] < min) {
                min = bbox[1];
            } else if (bbox == null && fontProgram.getFontMetrics().getTypoDescender() < min) {
                min = fontProgram.getFontMetrics().getTypoDescender();
            }
        }

        return min;
    }

    /**
     * Gets the descent of a char code in normalized 1000 units. The descent will always be
     * less than or equal to zero even if all the characters have an higher descent.
     *
     * @param ch the char code to get the descent of
     * @return the descent in normalized 1000 units
     */
    public int getDescent(int ch) {
        int min = 0;
        byte[] bytes = fontProgram.getEncoding().convertToBytes(ch);
        for (byte b : bytes) {
            int[] bbox = getFontProgram().getCharBBox(b & 0xff);
            if (bbox != null && bbox[1] < min) {
                min = bbox[1];
            } else if (bbox == null && fontProgram.getFontMetrics().getTypoDescender() < min) {
                min = fontProgram.getFontMetrics().getTypoDescender();
            }
        }

        return min;
    }

    /**
     * Gets the ascent of a {@code String} in normalized 1000 units. The ascent will always be
     * greater than or equal to zero even if all the characters have a lower ascent.
     *
     * @param text the {@code String} to get the ascent of
     * @return the ascent in normalized 1000 units
     */
    public int getAscent(String text) {
        int max = 0;
        byte[] bytes = fontProgram.getEncoding().convertToBytes(text);
        for (byte b : bytes) {
            int[] bbox = getFontProgram().getCharBBox(b & 0xff);
            if (bbox != null && bbox[3] > max) {
                max = bbox[3];
            } else if (bbox == null && fontProgram.getFontMetrics().getTypoAscender() > max) {
                max = fontProgram.getFontMetrics().getTypoAscender();
            }
        }

        return max;
    }

    /**
     * Gets the ascent of a char code in normalized 1000 units. The ascent will always be
     * greater than or equal to zero even if all the characters have a lower ascent.
     *
     * @param ch the char code to get the ascent of
     * @return the ascent in normalized 1000 units
     */
    public int getAscent(int ch) {
        int max = 0;
        byte[] bytes = fontProgram.getEncoding().convertToBytes(ch);
        for (byte b : bytes) {
            int[] bbox = getFontProgram().getCharBBox(b & 0xff);
            if (bbox != null && bbox[3] > max) {
                max = bbox[3];
            } else if (bbox == null && fontProgram.getFontMetrics().getTypoAscender() > max) {
                max = fontProgram.getFontMetrics().getTypoAscender();
            }
        }

        return max;
    }


    protected void setFontProgram(T fontProgram) {
        this.fontProgram = fontProgram;
    }

    protected abstract T initializeTypeFontForCopy(String encodingName) throws IOException;

    protected abstract T initializeTypeFont(String fontName, String encodingName) throws IOException;

    protected void init() throws IOException {
        PdfName baseFont = fontDictionary.getAsName(PdfName.BaseFont);
        getPdfObject().put(PdfName.Subtype, fontDictionary.getAsName(PdfName.Subtype));
        getPdfObject().put(PdfName.BaseFont, baseFont);
        PdfObject encodingObj = fontDictionary.get(PdfName.Encoding);
        initFontProgram(encodingObj);
        fontProgram.getFontNames().setFontName(baseFont.getValue());
        if (encodingObj == null) {
            if (FontConstants.BUILTIN_FONTS_14.contains(baseFont.getValue())) {
                fillEncoding(baseFont);
            } else {
                fillEncoding(null);
                CMapToUnicode toUnicode = processToUnicode();
                if (toUnicode != null) {
                    Map<Integer, Integer> rm = toUnicode.createReverseMapping();
                    for (Map.Entry<Integer, Integer> kv : rm.entrySet()) {
                        fontProgram.getEncoding().getSpecialMap().put(kv.getKey(), kv.getValue());
                        fontProgram.getEncoding().setUnicodeDifferences(kv.getValue(), (char)kv.getKey().intValue());
                    }
                }
            }

        } else if (encodingObj.isDictionary()) {

            PdfDictionary encDic = (PdfDictionary) encodingObj;
            PdfName baseEncoding = encDic.getAsName(PdfName.BaseEncoding);
            PdfDictionary enc = new PdfDictionary();
            enc.put(PdfName.Type, PdfName.Encoding);
            PdfArray diff = encDic.getAsArray(PdfName.Differences);

            if (diff != null) {
                enc.put(PdfName.Differences, diff);
            }

            if (baseEncoding == null) {
                fillEncoding(null);
            } else {
                fillEncoding(baseEncoding);
                enc.put(PdfName.BaseEncoding, baseEncoding);
            }

            getPdfObject().put(PdfName.Encoding, enc);
            fillDifference(diff);
        } else if (encodingObj.isName()) {
            getPdfObject().put(PdfName.Encoding, encodingObj);
            fillEncoding((PdfName) encodingObj);
        }

        PdfNumber firstChar = fontDictionary.getAsNumber(PdfName.FirstChar);
        PdfNumber lastChar = fontDictionary.getAsNumber(PdfName.LastChar);

        if (lastChar != null && firstChar != null) {
            getPdfObject().put(PdfName.FirstChar, firstChar);
            getPdfObject().put(PdfName.LastChar, lastChar);
        }

        PdfArray widths = fontDictionary.getAsArray(PdfName.Widths);
        if (widths != null) {
            getPdfObject().put(PdfName.Widths, widths);
            fontProgram.setWidths(getFillWidths(widths, firstChar, lastChar));
        }

        if (FontConstants.BUILTIN_FONTS_14.contains(fontProgram.getFontNames().getFontName())) {
            fontProgram = initializeTypeFont(fontProgram.getFontNames().getFontName(), fontProgram.getEncoding().getBaseEncoding());
        }

        PdfObject toUnicode = fontDictionary.get(PdfName.ToUnicode);
        if (toUnicode != null) {
            if (toUnicode instanceof PdfStream) {
                PdfStream newStream = (PdfStream) toUnicode.clone();
                getPdfObject().put(PdfName.ToUnicode, newStream);
                newStream.flush();
            }
        }


        PdfDictionary fromDescriptorDictionary = fontDictionary.getAsDictionary(PdfName.FontDescriptor);
        if (fromDescriptorDictionary != null) {
            PdfDictionary toDescriptorDictionary = getNewFontDescriptor(fromDescriptorDictionary);
            getPdfObject().put(PdfName.FontDescriptor, toDescriptorDictionary);
            toDescriptorDictionary.flush();
        }
    }

    protected  PdfDictionary getNewFontDescriptor(PdfDictionary fromDescriptorDictionary) {
        PdfDictionary toDescriptorDictionary = new PdfDictionary();
        toDescriptorDictionary.makeIndirect(getDocument());
        toDescriptorDictionary.put(PdfName.Type, PdfName.FontDescriptor);
        toDescriptorDictionary.put(PdfName.FontName, fromDescriptorDictionary.getAsName(PdfName.FontName));

        PdfName subtype = fromDescriptorDictionary.getAsName(PdfName.Subtype);
        if (subtype != null) {
            toDescriptorDictionary.put(PdfName.Subtype, subtype);
        }

        PdfNumber ascent = fromDescriptorDictionary.getAsNumber(PdfName.Ascent);
        if (ascent != null) {
            toDescriptorDictionary.put(PdfName.Ascent, ascent);
            fontProgram.getFontMetrics().setTypoAscender(ascent.getIntValue());
        }

        PdfNumber descent = fromDescriptorDictionary.getAsNumber(PdfName.Descent);
        if (descent != null) {
            toDescriptorDictionary.put(PdfName.Descent, ascent);
            fontProgram.getFontMetrics().setTypoDescender(descent.getIntValue());
        }

        PdfNumber capHeight = fromDescriptorDictionary.getAsNumber(PdfName.CapHeight);
        if (capHeight != null) {
            toDescriptorDictionary.put(PdfName.CapHeight, capHeight);
            fontProgram.getFontMetrics().setCapHeight(capHeight.getIntValue());
        }

        PdfNumber italicAngle = fromDescriptorDictionary.getAsNumber(PdfName.ItalicAngle);
        if (italicAngle != null) {
            toDescriptorDictionary.put(PdfName.ItalicAngle, italicAngle);
            fontProgram.getFontMetrics().setItalicAngle(italicAngle.getIntValue());
        }

        PdfNumber stemV = fromDescriptorDictionary.getAsNumber(PdfName.StemV);
        if (stemV != null) {
            toDescriptorDictionary.put(PdfName.StemV, stemV);
            fontProgram.getFontMetrics().setStemV(stemV.getIntValue());
        }

        PdfNumber fontWeight = fromDescriptorDictionary.getAsNumber(PdfName.FontWeight);
        if (fontWeight != null) {
            toDescriptorDictionary.put(PdfName.FontWeight, fontWeight);
        }


        PdfNumber flags = fromDescriptorDictionary.getAsNumber(PdfName.Flags);
        if (flags != null) {
            toDescriptorDictionary.put(PdfName.Flags, flags);
        }

        PdfStream fileStream = fromDescriptorDictionary.getAsStream(PdfName.FontFile);
        if (fileStream != null) {
            PdfStream newFileStream = (PdfStream) fileStream.clone();
            toDescriptorDictionary.put(PdfName.FontFile, newFileStream);
            newFileStream.flush();
        }

        PdfStream fileStream2 = fromDescriptorDictionary.getAsStream(PdfName.FontFile2);
        if (fileStream2 != null) {
            PdfStream newFileStream = (PdfStream) fileStream2.clone();
            toDescriptorDictionary.put(PdfName.FontFile2, newFileStream);
            newFileStream.flush();
        }

        PdfStream fileStream3 = fromDescriptorDictionary.getAsStream(PdfName.FontFile3);
        if (fileStream3 != null) {
            PdfStream newFileStream = (PdfStream) fileStream3.clone();
            toDescriptorDictionary.put(PdfName.FontFile3, newFileStream);
            newFileStream.flush();
        }

        PdfNumber leading = fromDescriptorDictionary.getAsNumber(PdfName.Leading);
        if (leading != null) {
            toDescriptorDictionary.put(PdfName.Leading, leading);
        }

        PdfNumber missingWidth = fromDescriptorDictionary.getAsNumber(PdfName.MissingWidth);
        if (missingWidth != null) {
            toDescriptorDictionary.put(PdfName.MissingWidth, missingWidth);
        }

        PdfNumber xHeight = fromDescriptorDictionary.getAsNumber(PdfName.XHeight);
        if (xHeight != null) {
            toDescriptorDictionary.put(PdfName.XHeight, xHeight);
            fontProgram.getFontMetrics().setXHeight(xHeight.getIntValue());
        }

        PdfName fontStretch = fromDescriptorDictionary.getAsName(PdfName.FontStretch);
        if (fontStretch != null) {
            toDescriptorDictionary.put(PdfName.FontStretch, fontStretch);
        }

        PdfString fontFamily = fromDescriptorDictionary.getAsString(PdfName.FontFamily);
        if (fontFamily != null) {
            toDescriptorDictionary.put(PdfName.FontFamily, fontFamily);
        }



        PdfDictionary fromStyleDictionary = fromDescriptorDictionary.getAsDictionary(PdfName.Style);
        if (fromStyleDictionary != null) {
            PdfDictionary toStyleDictionary = new PdfDictionary();
            PdfString panose = fromStyleDictionary.getAsString(PdfName.Panose);
            toStyleDictionary.put(PdfName.Panose, panose);
            toDescriptorDictionary.put(PdfName.Style, toStyleDictionary);
            fontProgram.getFontIdentification().setPanose(panose.toString());
        }


        PdfArray bbox = fromDescriptorDictionary.getAsArray(PdfName.FontBBox);
        toDescriptorDictionary.put(PdfName.FontBBox, bbox);

        if (bbox != null) {
            int llx = bbox.getAsNumber(0).getIntValue();
            int lly = bbox.getAsNumber(1).getIntValue();
            int urx = bbox.getAsNumber(2).getIntValue();
            int ury = bbox.getAsNumber(3).getIntValue();
            if (llx > urx) {
                int t = llx;
                llx = urx;
                urx = t;
            }
            if (lly > ury) {
                int t = lly;
                lly = ury;
                ury = t;
            }
            fontProgram.getFontMetrics().getBbox().setBbox(llx, lly, urx, ury);
        }

        return toDescriptorDictionary;
    }

    private String getEncodingName(PdfName encoding) {
        String encodingName = PdfEncodings.WINANSI;
        if (PdfName.MacRomanEncoding.equals(encoding)) {
            encodingName = PdfEncodings.MACROMAN;
        } else if (FontConstants.SYMBOL.equals(encoding.getValue())) {
            encodingName = FontConstants.SYMBOL;
        } else if (FontConstants.ZAPFDINGBATS.equals(encoding.getValue())) {
            encodingName = FontConstants.ZAPFDINGBATS;
        }
        return encodingName;
    }

    private void initFontProgram(PdfObject encoding) throws IOException {
        if (encoding == null) {
            fontProgram = initializeTypeFontForCopy(PdfEncodings.EmptyString);
        } else if (encoding.isName()) {
            PdfName encodingPdfName = (PdfName) encoding;
            fontProgram = initializeTypeFontForCopy(getEncodingName(encodingPdfName));
        } else if (encoding.isDictionary()) {
            PdfDictionary encDic = (PdfDictionary) encoding;
            PdfName baseEncodingName = encDic.getAsName(PdfName.BaseEncoding);
            if (baseEncodingName == null) {
                fontProgram = initializeTypeFontForCopy(PdfEncodings.EmptyString);
            } else {
                fontProgram = initializeTypeFontForCopy(getEncodingName(baseEncodingName));
            }
        }
    }

    private void fillEncoding(PdfName encoding) {
        String encodingString = encoding != null ? encoding.getValue() : null;
        if (encoding == null && isSymbolic()) {
            for (int k = 0; k < 256; ++k) {
                fontProgram.getEncoding().getSpecialMap().put(k, k);
                fontProgram.getEncoding().setUnicodeDifferences(k, (char) k);
            }
        } else if (PdfName.MacRomanEncoding.equals(encoding) || PdfName.WinAnsiEncoding.equals(encoding)
                || FontConstants.SYMBOL.equals(encodingString) || FontConstants.ZAPFDINGBATS.equals(encodingString)) {

            byte[] b = new byte[256];
            for (int k = 0; k < 256; ++k) {
                b[k] = (byte) k;
            }

            String cv = PdfEncodings.convertToString(b, fontProgram.getEncoding().getBaseEncoding());
            char[] arr = cv.toCharArray();
            for (int k = 0; k < 256; ++k) {
                fontProgram.getEncoding().getSpecialMap().put(arr[k], k);
                fontProgram.getEncoding().setUnicodeDifferences(k, arr[k]);
            }
        } else {
            for (int k = 0; k < 256; ++k) {
                fontProgram.getEncoding().getSpecialMap().put(PdfEncodings.standardEncoding[k], k);
                fontProgram.getEncoding().setUnicodeDifferences(k, (char) PdfEncodings.standardEncoding[k]);
            }
        }
    }

    private void fillDifference(PdfArray diffs) {
        if (diffs != null) {
            int currentNumber = 0;
            for (int k = 0; k < diffs.size(); ++k) {
                PdfObject obj = diffs.get(k);
                if (obj.isNumber())
                    currentNumber = ((PdfNumber) obj).getIntValue();
                else {
                    Integer c = AdobeGlyphList.nameToUnicode(((PdfName) obj).getValue());
                    if (c != null) {
                        fontProgram.getEncoding().getSpecialMap().put(c, currentNumber);
                        fontProgram.getEncoding().setDifferences(currentNumber, ((PdfName) obj).getValue());
                        fontProgram.getEncoding().setUnicodeDifferences(currentNumber, (char) (int)c);
                    } else {
                        CMapToUnicode toUnicode = processToUnicode();
                        if (toUnicode == null) {
                            toUnicode = new CMapToUnicode();
                        }

                        final String unicode = toUnicode.lookup(new byte[]{(byte) currentNumber}, 0, 1);
                        if ((unicode != null) && (unicode.length() == 1)) {
                            fontProgram.getEncoding().getSpecialMap().put(unicode.charAt(0), currentNumber);
                            fontProgram.getEncoding().setDifferences(currentNumber, String.valueOf(unicode.charAt(0)));
                            fontProgram.getEncoding().setUnicodeDifferences(unicode.charAt(0), (char) currentNumber);
                        }
                    }
                    ++currentNumber;
                }
            }
        }
    }

    private CMapToUnicode processToUnicode() {
        CMapToUnicode cMapToUnicode = null;
        PdfObject toUni = this.fontDictionary.get(PdfName.ToUnicode);
        if (toUni instanceof PdfStream) {
            try {
                byte[] uniBytes = ((PdfStream) toUni).getBytes();
                CMapLocation lb = new CMapLocationFromBytes(uniBytes);
                cMapToUnicode = new CMapToUnicode();
                CMapParser.parseCid("", cMapToUnicode, lb);
            } catch (Exception e) {
                cMapToUnicode = null;
            }
        }
        return cMapToUnicode;
    }


}
