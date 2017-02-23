package org.pbrt.openexr.types;

import java.util.ArrayList;
import java.util.List;

import org.pbrt.openexr.header.Attribute;
import org.pbrt.openexr.util.DataReader;

/**
 * Copyright (c) Bartosz Zaczynski, 2010
 * http://syntaxcandy.blogspot.com
 */
public class Channels extends Attribute {
	
	public List<Channel> list = new ArrayList<Channel>();
	
	public Channels(DataReader data) {
		for (;;) {
			if (data.peekByte() == 0) {
				data.skip(1);
				break;
			} else {
				list.add(new Channel(data));
			}
		}
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(name);
		sb.append(" = [");
		appendChannels(sb);
		sb.append("]");
		return sb.toString();
	}
	
	private void appendChannels(StringBuilder sb) {
		int length = list.size();
		for (int i = 0; i < length; i++) {
			Channel channel = list.get(i);
			sb.append(channel + ((i < length - 1) ? ", " : ""));
		}
	}
}