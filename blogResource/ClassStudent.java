package com.sahadev.bean;

/**
 * Created by shangbin on 2016/11/24.
 * Email: sahadev@foxmail.com
 */

public class ClassStudent {
	private String name;

	public ClassStudent() {

	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName(){
		return this.name + ".Mr";	
	}
}
