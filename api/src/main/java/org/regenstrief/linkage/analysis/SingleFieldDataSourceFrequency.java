package org.regenstrief.linkage.analysis;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class SingleFieldDataSourceFrequency extends DataSourceFrequency {
	
	private final Map<String, Count> frequencies = new HashMap<String, Count>();
	
	private final Set<String> fields;
	
	public SingleFieldDataSourceFrequency(final String field) {
		fields = Collections.singleton(field);
	}
	
	@Override
	public final int getFrequency(final String field, final String token) {
		final Count count = frequencies.get(token);
		return (count == null) ? 0 : count.i;
	}
	
	@Override
	public final void setFrequency(final String field, final String token, final int freq) {
		final Count count = frequencies.get(token);
		if (count == null) {
			frequencies.put(token, new Count(freq));
		} else {
			count.i = freq;
		}
	}
	
	@Override
	public final void incrementCount(final String field, final String token) {
		final Count count = frequencies.get(token);
		if (count == null) {
			frequencies.put(token, new Count(1));
		} else {
			count.i++;
		}
	}
	
	@Override
	public final Set<String> getTokens(final String field) {
		return frequencies.keySet();
	}
	
	@Override
	public final Set<String> getFields() {
		return fields;
	}
}
