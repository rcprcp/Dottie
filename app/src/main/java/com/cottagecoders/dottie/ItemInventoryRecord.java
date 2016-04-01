package com.cottagecoders.dottie;


public class ItemInventoryRecord {
	int id;
	String category;
	 int noofitems;
	
    public  ItemInventoryRecord(
            int id,
            int noofitems,
    		String category
            )
    
    {
    	    this.id = id;
    	    this.category = category;
    		this.noofitems = noofitems;
    		
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getNoofitems() {
		return noofitems;
	}

	public void setNoofitems(int noofitems) {
		this.noofitems = noofitems;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	

}
