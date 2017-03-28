package org.pbrt.openexr.header;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.pbrt.openexr.util.DataReader;

/**
 * Copyright (c) Bartosz Zaczynski, 2010
 * http://syntaxcandy.blogspot.com
 */
public class Header {
	
	private Map<String, Attribute> attributes = new HashMap<>();
	
	public Header(DataReader data) throws IOException {
		for (;;) {
			if (data.peekByte() == 0) {
				data.skip(1);
				break;
			} else {
				addAttribute(data);
			}
		}
	}
	
	public Attribute get(String name) {
		return attributes.get(name);
	}
	
	public List<Attribute> getAll() {
		
		List<Attribute> all = new ArrayList<>();
		for (String name : attributes.keySet()) {
			all.add(get(name));
		}
		
		all.sort(Comparator.comparing(x -> x.name));
		
		return all;
	}
	
	private void addAttribute(DataReader data) throws IOException {
		
		String name = data.readString();
		String type = data.readString();
		int size = data.readInt();
		
		Attribute attribute = Attribute.newInstance(data, name, type, size);
		attributes.put(name, attribute);
	}
}