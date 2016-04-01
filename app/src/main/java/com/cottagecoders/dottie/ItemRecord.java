package com.cottagecoders.dottie;

public class ItemRecord {
	int id;
	String type;
	String name;
	String category;
	String verb;
	String unicode1;
	String unicode2;
	String unicode3;
	int used;
	String plural;
	String article;

	public ItemRecord(int id, String type, String name, String category,
			String verb, String unicode1, String unicode2, String unicode3,
			int used, String plural, String article)

	{
		this.id = id;
		this.type = type;
		this.name = name;
		this.category = category;
		this.verb = verb;
		this.unicode1 = unicode1;
		this.unicode2 = unicode2;
		this.unicode3 = unicode3;
		this.used = used;
		this.plural = plural;
		this.article = article;

	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getUnicode1() {
		return unicode1;
	}

	public void setUnicode1(String unicode1) {
		this.unicode1 = unicode1;
	}

	public String getUnicode2() {
		return unicode2;
	}

	public void setUnicode2(String unicode2) {
		this.unicode2 = unicode2;
	}

	public String getUnicode3() {
		return unicode3;
	}

	public void setUnicode3(String unicode3) {
		this.unicode3 = unicode3;
	}

	public int getUsed() {
		return used;
	}

	public void setUsed(int used) {
		this.used = used;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getVerb() {
		return verb;
	}

	public void setPlural(String plural) {
		this.plural = plural;
	}

	public String getPlural() {
		return plural;
	}

	public void setArticle(String article) {
		this.article = article;
	}

	public String getArticle() {
		return article;
	}

}
