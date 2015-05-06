package llvmast;
public class LlvmBool extends LlvmValue{
	public int val;
    public LlvmBool(int B){
    	type = LlvmPrimitiveType.I1;
    	this.val = B;
    }
    
    public String toString(){
    	switch (this.val){
    	case FALSE : {return "0";}
    	case TRUE  : {return "1";}
    		
    	}
		return null;
    }
    
    public static final int FALSE  = 0;
    public static final int TRUE  = 1;
    
}
