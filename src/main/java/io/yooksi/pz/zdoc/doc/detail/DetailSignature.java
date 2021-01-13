/*
 * ZomboidDoc - Project Zomboid API parser and lua compiler.
 * Copyright (C) 2020 Matthew Cain
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.yooksi.pz.zdoc.doc.detail;

import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.jsoup.nodes.Element;

import io.yooksi.pz.zdoc.element.java.JavaClass;

abstract class DetailSignature {

	final String signature;

	DetailSignature(String signature) {
		this.signature = normalizeSignature(signature);
	}

	/**
	 * Normalize given signature text:
	 * <ul>
	 * <li>Convert non-breaking space ({@code &nbsp}) to whitespace.</li>
	 * <li>Remove newlines.</li>
	 * <li>Remove consecutive whitespaces.</li>
	 * </ul>
	 *
	 * @param text {@code String} to normalize.
	 * @return normalized text.
	 */
	private static String normalizeSignature(String text) {

		StringBuilder sb = new StringBuilder();
		char lastChar = 0;
		for (char c : text.toCharArray())
		{
			// convert &nbsp to whitespace
			if (c == 160) {
				c = ' ';
			}
			// remove newlines
			else if (c == '\r' || c == '\n') {
				continue;
			}
			// remove consecutive whitespaces
			else if (lastChar == ' ' && c == ' ') {
				continue;
			}
			sb.append(c);
			lastChar = c;
		}
		return sb.toString();
	}

	/**
	 * Gets the combined text of the given element and all its children.
	 * Whitespace is normalized and trimmed. The normalization process also
	 * includes converting non-breaking space ({@code &nbsp}) to whitespace.
	 *
	 * @param element {@code Element} text to normalize.
	 * @return normalized and trimmed text or empty text if none.
	 *
	 * @see Element#text()
	 */
	static String normalizeElement(Element element) {
		return normalizeSignature(element.text());
	}

	public static @Nullable JavaClass parseClassSignature(String signature) {

		List<JavaClass> result = new TypeSignatureParser(signature).parse();
		return !result.isEmpty() ? result.get(0) : null;
	}

	@Override
	public String toString() {
		return signature;
	}
}
