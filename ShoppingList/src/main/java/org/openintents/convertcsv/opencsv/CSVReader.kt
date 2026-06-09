/*
 * Copyright (C) 2008 OpenIntents.org
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
 */

/*
 * Copyright 2005 Bytecode Pty Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The project is available at http://sourceforge.net/projects/opencsv/
 *
 * Modifications:
 * - Peli: Dec 12, 2008: Remove "this.".
 */

package org.openintents.convertcsv.opencsv

import org.openintents.convertcsv.common.ConvertCsvBaseActivity

import java.io.BufferedReader
import java.io.IOException
import java.io.Reader
import java.util.ArrayList

/**
 * A very simple CSV reader released under a commercial-friendly license.
 *
 * @author Glen Smith
 */
class CSVReader(reader: Reader, separator: Char, quotechar: Char, line: Int) {

    companion object {
        /** The default separator to use if none is supplied to the constructor. */
        const val DEFAULT_SEPARATOR: Char = ','

        /**
         * The default quote character to use if none is supplied to the constructor.
         */
        const val DEFAULT_QUOTE_CHARACTER: Char = '"'

        /**
         * The default line to start reading.
         */
        const val DEFAULT_SKIP_LINES: Int = 0
    }

    private val br: BufferedReader = BufferedReader(reader)
    private var hasNext: Boolean = true
    private val separator: Char = separator
    private val quotechar: Char = quotechar
    private val skipLines: Int = line
    private var readSoFar: Int = 0
    private var linesSkiped: Boolean = false

    /**
     * Constructs CSVReader using a comma for the separator.
     *
     * @param reader the reader to an underlying CSV source.
     */
    constructor(reader: Reader) : this(reader, DEFAULT_SEPARATOR)

    /**
     * Constructs CSVReader with supplied separator.
     *
     * @param reader    the reader to an underlying CSV source.
     * @param separator the delimiter to use for separating entries.
     */
    constructor(reader: Reader, separator: Char) : this(reader, separator, DEFAULT_QUOTE_CHARACTER)

    /**
     * Constructs CSVReader with supplied separator and quote char.
     *
     * @param reader    the reader to an underlying CSV source.
     * @param separator the delimiter to use for separating entries
     * @param quotechar the character to use for quoted elements
     */
    constructor(reader: Reader, separator: Char, quotechar: Char) :
            this(reader, separator, quotechar, DEFAULT_SKIP_LINES)

    /**
     * Reads the entire file into a List with each element being a String[] of tokens.
     *
     * @return a List of String[], with each String[] representing a line of the file.
     * @throws IOException if bad things happen during the read
     */
    @Throws(IOException::class)
    fun readAll(): List<Array<String>> {
        val allElements = ArrayList<Array<String>>()
        while (hasNext) {
            val nextLineAsTokens = readNext()
            if (nextLineAsTokens != null)
                allElements.add(nextLineAsTokens)
        }
        return allElements
    }

    /**
     * Reads the next line from the buffer and converts to a string array.
     *
     * @return a string array with each comma-separated element as a separate entry.
     * @throws IOException if bad things happen during the read
     */
    @Throws(IOException::class)
    fun readNext(): Array<String>? {
        val nextLine = getNextLine()
        return if (hasNext) parseLine(nextLine) else null
    }

    /**
     * Reads the next line from the file.
     *
     * @return the next line from the file without trailing newline
     * @throws IOException if bad things happen during the read
     */
    @Throws(IOException::class)
    private fun getNextLine(): String? {
        if (!linesSkiped) {
            for (i in 0 until skipLines) {
                val skippedLine = br.readLine()
                if (skippedLine != null) {
                    readSoFar += skippedLine.length + 1    // This is an approximation, CR/LF might count as 2
                    ConvertCsvBaseActivity.dispatchConversionProgress(readSoFar)
                }
            }
            linesSkiped = true
        }
        val nextLine = br.readLine()
        if (nextLine == null) {
            hasNext = false
        } else {
            readSoFar += nextLine.length + 1    // This is an approximation, CR/LF might count as 2
            ConvertCsvBaseActivity.dispatchConversionProgress(readSoFar)
        }
        return if (hasNext) nextLine else null
    }

    /**
     * Parses an incoming String and returns an array of elements.
     *
     * @param nextLine the string to parse
     * @return the comma-tokenized list of elements, or null if nextLine is null
     * @throws IOException if bad things happen during the read
     */
    @Throws(IOException::class)
    private fun parseLine(nextLine: String?): Array<String>? {
        var line = nextLine ?: return null

        val tokensOnThisLine = ArrayList<String>()
        var sb = StringBuffer()
        var inQuotes = false
        do {
            if (inQuotes) {
                // continuing a quoted section, reappend newline
                sb.append("\n")
                line = getNextLine() ?: break
            }
            var i = 0
            while (i < line.length) {
                val c = line[i]
                if (c == quotechar) {
                    // this gets complex... the quote may end a quoted block, or escape another quote.
                    // do a 1-char lookahead:
                    if (inQuotes  // we are in quotes, therefore there can be escaped quotes in here.
                        && line.length > (i + 1)  // there is indeed another character to check.
                        && line[i + 1] == quotechar
                    ) { // ..and that char. is a quote also.
                        // we have two quote chars in a row == one quote char, so consume them both and
                        // put one on the token. we do *not* exit the quoted text.
                        sb.append(line[i + 1])
                        i++
                    } else {
                        inQuotes = !inQuotes
                        // the tricky case of an embedded quote in the middle: a,bc"d"ef,g
                        if (i > 2 //not on the begining of the line
                            && line[i - 1] != separator //not at the begining of an escape sequence
                            && line.length > (i + 1) &&
                            line[i + 1] != separator //not at the	end of an escape sequence
                        ) {
                            sb.append(c)
                        }
                    }
                } else if (c == separator && !inQuotes) {
                    tokensOnThisLine.add(sb.toString())
                    sb = StringBuffer() // start work on next token
                } else {
                    sb.append(c)
                }
                i++
            }
        } while (inQuotes)
        tokensOnThisLine.add(sb.toString())
        return tokensOnThisLine.toTypedArray()
    }

    /**
     * Closes the underlying reader.
     *
     * @throws IOException if the close fails
     */
    @Throws(IOException::class)
    fun close() {
        br.close()
    }
}
