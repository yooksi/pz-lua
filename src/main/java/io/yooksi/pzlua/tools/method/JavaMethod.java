package io.yooksi.pzlua.tools.method;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;

import org.jetbrains.annotations.Nullable;

import io.yooksi.pzlua.tools.lang.ElementParser;
import io.yooksi.pzlua.tools.lang.ParseRegex;

/**
 * Textual representation of Java method.
 */
public class JavaMethod extends Method {

	public JavaMethod(String modifier, String returnType, String name, Parameter[] params) {
		super(modifier, returnType, name, params);
	}

	@Override
	public String toString() {

		String sParams = "";
		if (this.params.length > 0)
		{
			final StringBuilder sb = new StringBuilder();
			Arrays.stream(params).forEach(p -> sb.append(p.getUnqualified().toString()).append(", "));
			sParams = sb.substring(0, sb.length() - 2).trim();
		}
		return String.format("%s%s%s %s(%s)", modifier,
				(modifier.length() > 0 ? ' ' : ""), returnType, name, sParams);
	}

	public static class Parser implements ElementParser<JavaMethod> {

		@Override
		public @Nullable JavaMethod parse(String text) {
			Matcher matcher = ParseRegex.JAVA_METHOD_REGEX.matcher(text);
			if (matcher.find())
			{
				java.util.List<Parameter> paramList = new ArrayList<>();
				String paramsMatched = matcher.group(4);
				if (paramsMatched != null)
				{
					for (String param : paramsMatched.trim().split(",(?:\\s+)?"))
					{
						String[] params = param.split("\\s+");
						paramList.add(new Parameter(params[0], params.length < 2 ? "" : params[1]));
					}
				}
				return new JavaMethod(ParseRegex.getMatchedGroup(matcher, 1),
						ParseRegex.getMatchedGroup(matcher, 2),
						ParseRegex.getMatchedGroup(matcher, 3),
						paramList.toArray(new Parameter[]{}));
			}
			else return null;
		}
	}
}
