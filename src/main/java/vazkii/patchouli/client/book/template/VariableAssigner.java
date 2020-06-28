package vazkii.patchouli.client.book.template;

import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import org.apache.commons.lang3.text.WordUtils;

import vazkii.patchouli.api.IComponentProcessor;
import vazkii.patchouli.api.IVariable;
import vazkii.patchouli.api.IVariableProvider;
import vazkii.patchouli.api.IVariablesAvailableCallback;
import vazkii.patchouli.common.util.EntityUtil;
import vazkii.patchouli.common.util.ItemStackUtil;

import javax.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VariableAssigner {

	private static final Pattern INLINE_VAR_PATTERN = Pattern.compile("([^#]*)(#[^#]+)#(.*)");
	private static final Pattern FUNCTION_PATTERN = Pattern.compile("(.+)->(.+)");

	private static final Map<String, Function<IVariable, IVariable>> FUNCTIONS = new HashMap<>();
	static {
		FUNCTIONS.put("iname",    VariableAssigner::iname);
		FUNCTIONS.put("icount",   VariableAssigner::icount);
		FUNCTIONS.put("ename",    wrapStringFunc(VariableAssigner::ename));
		FUNCTIONS.put("lower",    wrapStringFunc(String::toLowerCase));
		FUNCTIONS.put("upper",    wrapStringFunc(String::toUpperCase));
		FUNCTIONS.put("trim",     wrapStringFunc(String::trim));
		FUNCTIONS.put("capital",  wrapStringFunc(WordUtils::capitalize));
		FUNCTIONS.put("fcapital", wrapStringFunc(WordUtils::capitalizeFully));
		FUNCTIONS.put("i18n",     wrapStringFunc(I18n::format));
		FUNCTIONS.put("exists",   VariableAssigner::exists);
		FUNCTIONS.put("iexists",  VariableAssigner::iexists);
		FUNCTIONS.put("inv",      VariableAssigner::inv);
	}

	public static void assignVariableHolders(IVariablesAvailableCallback object, IVariableProvider variables, IComponentProcessor processor, TemplateInclusion encapsulation) {
		Context c = new Context(variables, processor, encapsulation);
		object.onVariablesAvailable(key -> {
			IVariable resolved = resolveString(key, c);
			return resolved != null ? resolved : IVariable.wrap(key);
		});
	}

	private static IVariable resolveString(@Nullable String curr, Context c) {
		if (curr == null || curr.isEmpty()) {
			return null;
		}

		String s = curr;
		Matcher m = INLINE_VAR_PATTERN.matcher(s);
		while (m.matches()) {
			String before = m.group(1);
			String var = m.group(2);
			String after = m.group(3);

			String resolved = resolveStringFunctions(var, c).asString();

			s = String.format("%s%s%s", before, resolved, after);
			m = INLINE_VAR_PATTERN.matcher(s);
		}

		return resolveStringFunctions(s, c);
	}

	private static IVariable resolveStringFunctions(String curr, Context c) {
		IVariable cached = c.getCached(curr);
		if (cached != null) {
			return cached;
		}

		Matcher m = FUNCTION_PATTERN.matcher(curr);

		if (m.matches()) {
			String funcStr = m.group(2);
			String arg = m.group(1);

			if (FUNCTIONS.containsKey(funcStr)) {
				Function<IVariable, IVariable> func = FUNCTIONS.get(funcStr);
				IVariable parsedArg = resolveStringFunctions(arg, c);
				return c.cache(curr, func.apply(parsedArg));
			} else {
				throw new IllegalArgumentException("Invalid Function " + funcStr);
			}
		}

		IVariable ret = resolveStringVar(curr, c);

		return c.cache(curr, ret);
	}

	private static IVariable resolveStringVar(String original, Context c) {
		String curr = original;
		IVariable val = null;

		if(curr == null) {
			return IVariable.empty();
		}

		if(curr.startsWith("#")) {
			if (c.encapsulation != null) {
				val = c.encapsulation.attemptVariableLookup(curr);
				if (val != null) {
					return val;
				}
				curr = c.encapsulation.qualifyName(curr);
			}

			String key = curr.substring(1);
			String originalKey = original.substring(1);

			if (c.processor != null) {
				val = c.processor.process(originalKey);
			}

			if (val == null && c.variables.has(key)) {
				val = c.variables.get(key);
			}

			if(val != null) {
				return val;
			}
		}
		return IVariable.wrap(curr);
	}

	private static Function<IVariable, IVariable> wrapStringFunc(Function<String, String> inner) {
		Function<IVariable, String> unwrap = IVariable::asString;
		return unwrap.andThen(inner).andThen(IVariable::wrap);
	}

	private static IVariable iname(IVariable arg) {
		ItemStack stack = arg.as(ItemStack.class);
		return IVariable.wrap(stack.getDisplayName().getFormattedText());
	}

	private static IVariable icount(IVariable arg) {
		ItemStack stack = arg.as(ItemStack.class);
		return IVariable.wrap(stack.getCount());
	}

	private static IVariable exists(IVariable arg) {
		return IVariable.wrap(!arg.unwrap().isJsonNull());
	}

	private static IVariable iexists(IVariable arg) {
		ItemStack stack = arg.as(ItemStack.class);
		return IVariable.wrap(stack != null && !stack.isEmpty());
	}

	private static IVariable inv(IVariable arg) {
		return IVariable.wrap(!arg.unwrap().getAsBoolean());
	}

	private static String ename(String arg) {
		return EntityUtil.getEntityName(arg);
	}

	private static class Context {

		final IVariableProvider variables;
		final IComponentProcessor processor;
		final TemplateInclusion encapsulation;
		final Map<String, IVariable> cachedVars = new HashMap<>();

		Context(IVariableProvider variables, IComponentProcessor processor, TemplateInclusion encapsulation) {
			this.variables = variables;
			this.processor = processor;
			this.encapsulation = encapsulation;
		}

		IVariable getCached(String s) {
			return cachedVars.get(s);
		}

		IVariable cache(String k, IVariable v) {
			cachedVars.put(k, v);
			return v;
		}

	}

}
