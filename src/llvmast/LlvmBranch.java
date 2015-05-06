package llvmast;
public  class LlvmBranch extends LlvmInstruction{

    public LlvmLabelValue ifTrue = null;
    public LlvmLabelValue ifFalse = null;
    public LlvmValue cond = null;
    
	
	public LlvmBranch(LlvmLabelValue label){
		this.ifTrue = label;
    }
    
    public LlvmBranch(LlvmValue cond,  LlvmLabelValue brTrue, LlvmLabelValue brFalse){
    	this.ifTrue = brTrue;
    	this.ifFalse = brFalse;
    	this.cond = cond;
    }

    public String toString(){
		if (cond == null && ifTrue != null) {
			return " " + "br label %" + ifTrue;
		} else {
			return " " + "br i1 " + cond + ", label %" + ifTrue + ", label %" + ifFalse;
		}
    }
}