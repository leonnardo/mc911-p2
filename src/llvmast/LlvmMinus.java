package llvmast;

public  class LlvmMinus extends LlvmInstruction{
	
	public LlvmRegister lhs;
	public LlvmType type;
	public LlvmValue op1, op2;
	
	public LlvmMinus(LlvmRegister lhs, LlvmType type, LlvmValue op1, LlvmValue op2){
		this.lhs = lhs;
		
    }

    public String toString(){
		return null;
    }
}
