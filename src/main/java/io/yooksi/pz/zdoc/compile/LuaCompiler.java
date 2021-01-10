/*
 * ZomboidDoc - Project Zomboid API parser and lua compiler.
 * Copyright (C) 2021 Matthew Cain
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
package io.yooksi.pz.zdoc.compile;

import java.util.*;

import org.apache.commons.collections4.PredicateUtils;
import org.apache.commons.collections4.set.PredicatedSet;
import org.jetbrains.annotations.UnmodifiableView;

import io.yooksi.pz.zdoc.doc.ZomboidJavaDoc;
import io.yooksi.pz.zdoc.doc.ZomboidLuaDoc;
import io.yooksi.pz.zdoc.element.IClass;
import io.yooksi.pz.zdoc.element.IField;
import io.yooksi.pz.zdoc.element.IMethod;
import io.yooksi.pz.zdoc.element.IParameter;
import io.yooksi.pz.zdoc.element.lua.*;

public class LuaCompiler implements ICompiler<ZomboidLuaDoc> {

	private static final Map<String, String> CACHED_LUA_TYPES = new HashMap<>();
	private static final Map<String, LuaClass> GLOBAL_CLASSES = new HashMap<>();

	private final @UnmodifiableView Set<ZomboidJavaDoc> javaDocs;

	public LuaCompiler(Set<ZomboidJavaDoc> javaDocs) {
		this.javaDocs = Collections.unmodifiableSet(javaDocs);
	}

	private static LuaType resolveLuaType(IClass iClass) throws CompilerException {

		List<LuaType> otherTypes = new ArrayList<>();
		for (IClass typeParam : iClass.getTypeParameters())
		{
			if (typeParam != null)
			{
				String paramName = typeParam.getName();
				String typeName = CACHED_LUA_TYPES.get(paramName);
				if (typeName == null)
				{
					typeName = resolveClassName(paramName, false);
					CACHED_LUA_TYPES.put(paramName, typeName);
				}
				otherTypes.add(new LuaType(typeName));
			}
			else otherTypes.add(new LuaType("Unknown"));
		}
		String className = CACHED_LUA_TYPES.get(iClass.getName());
		if (className == null)
		{
			className = resolveClassName(iClass.getName(), false);
			CACHED_LUA_TYPES.put(iClass.getName(), className);
		}
		return new LuaType(className, otherTypes);
	}

	private static LuaClass resolveLuaClass(String name) throws CompilerException {

		LuaClass cachedClass = GLOBAL_CLASSES.get(name);
		if (cachedClass == null)
		{
			String parentType = name.replace('$', '.');
			LuaClass result = new LuaClass(resolveClassName(name, true), parentType);
			GLOBAL_CLASSES.put(name, result);
			return result;
		}
		else return cachedClass;
	}

	private static String resolveClassName(String signature, boolean global) throws CompilerException {

		String[] packages = signature.split("\\.");
		if (packages.length > 1)
		{
			String result;
			char[] cName = packages[packages.length - 1].toCharArray();
			StringBuilder sb = new StringBuilder();
			for (char c : cName) {
				sb.append(c == '$' ? '.' : c);
			}
			result = sb.toString();
			if (global)
			{
				for (int i = packages.length - 2; i >= 0 && GLOBAL_CLASSES.containsKey(result); i--) {
					result = packages[i] + '_' + result;
				}
				if (GLOBAL_CLASSES.containsKey(result)) {
					throw new CompilerException(String.format("Unexpected class name (%s) " +
							"duplicate detected - %s", result, GLOBAL_CLASSES.toString()));
				}
			}
			return result;
		}
		// class does not reside in a package
		else return signature;
	}

	public Set<ZomboidLuaDoc> compile() throws CompilerException {

		Set<ZomboidLuaDoc> result = PredicatedSet.predicatedSet(
				new HashSet<>(), PredicateUtils.notNullPredicate()
		);
		/* represents ? parameter type
		 * since EmmyLua does not have a good format for notating parameterized types
		 * this is the best way we can note an unknown parameter type
		 */
		result.add(new ZomboidLuaDoc(new LuaClass("Unknown")));
		for (ZomboidJavaDoc javaDoc : javaDocs)
		{
			LuaClass luaClass = resolveLuaClass(javaDoc.getName());
			GLOBAL_CLASSES.put(luaClass.getName(), luaClass);

			List<LuaField> luaFields = new ArrayList<>();
			for (IField field : javaDoc.getFields())
			{
				LuaType fieldType = resolveLuaType(field.getType());
				luaFields.add(new LuaField(fieldType, field.getName(), field.getModifier()));
			}
			Set<LuaMethod> luaMethods = new HashSet<>();
			for (IMethod method : javaDoc.getMethods())
			{
				LuaType returnType = resolveLuaType(method.getReturnType());

				List<LuaParameter> parameters = new ArrayList<>();
				for (IParameter param : method.getParams())
				{
					LuaType paramClass = resolveLuaType(param.getType());
					parameters.add(new LuaParameter(paramClass, param.getName()));
				}
				luaMethods.add(new LuaMethod(method.getName(),
						luaClass, method.getModifier(), returnType, parameters));
			}
			result.add(new ZomboidLuaDoc(luaClass, luaFields, luaMethods));
		}
		return result;
	}
}
