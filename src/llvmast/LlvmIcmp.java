package llvmast;
public  class LlvmIcmp extends LlvmInstruction{
    
	private LlvmRegister lhs;
	private String condition;
	private LlvmType type;
	private LlvmValue op1;
	private LlvmValue op2;
	public static final int EQ = 1;
	public static final int NE = 2;
	public static final int UGT = 3;
	public static final int UGE = 4; 
	public static final int ULT = 5;
	public static final int ULE = 6;
	public static final int SGT = 7;
	public static final int SGE = 8;
	public static final int SLT = 9;
	public static final int SLE = 10;
	
	
    public LlvmIcmp(LlvmRegister lhs,  int conditionCode, LlvmType type, LlvmValue op1, LlvmValue op2){
    	this.lhs = lhs;
    	this.type = type;
    	this.op1 = op1;
    	this.op2 = op2;
    	
    	switch (conditionCode) {
    		case EQ: this.condition = "eq"; break;
    		case NE: this.condition = "ne"; break;
    		case UGT: this.condition = "ugt"; break;
    		case UGE: this.condition = "uge"; break;
    		case ULT: this.condition = "ult"; break;
    		case ULE: this.condition = "ule"; break;
    		case SGT: this.condition = "sgt"; break;
    		case SGE: this.condition = "sge"; break;
    		case SLT: this.condition = "slt"; break;
    		case SLE: this.condition = "sle"; break;
    	}
    	
    }

    public String toString(){
		return " " + lhs + " = icmp " + condition + " " + type + " " + op1 + ", " + op2;
    }
}