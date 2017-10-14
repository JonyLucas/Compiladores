package analisador_sintatico;

import java.util.ArrayList;
import java.util.Stack;
import java.util.HashMap;
import java.util.ArrayList;

public class AnalisadorSintatico {
	
	private ArrayList<String[]> token_table; //Tabela de token retornada pelo analisador lexico
	private Stack<String> symbol_table; //Pilha para implementar a tabela de simbolos
	private Stack<String> type_stack; //Pilha para implementar a tabela de tipos, a uniao entre symbol_table e type_table funcionaria como um HashMap, onde o indice mapeia os elementos das duas pilhas
	private Stack<String> type_control_stack; //Pilha de controle de tipo (PcT), ser� usada nos comandos de atribui�ao, expressao aritmetica, relacional e logica
	private Stack<String> operation_stack; //Pilha para armazenar as opera�oes, forma que utilizei para solucionar a verifica�ao de tipos
	private HashMap<String, ArrayList<String>> procedures_parameters; //Usado para fazer a checagem de tipos na ativa��o de procedimentos
	private int nLine; //Linha corrente de leitura da tabela
	private String[] token; //Variavel utilizada para resgatar uma linha da tabela de tokens
	
	public AnalisadorSintatico(ArrayList<String[]> al){
		token_table = al;
		symbol_table =  new Stack<String>();
		type_stack = new Stack<String>();
		type_control_stack = new Stack<String>();
		operation_stack = new Stack<String>();
		procedures_parameters = new HashMap<String, ArrayList<String>>();
		nLine = 0;
	}
	
	public void analisar(){
		System.out.println("Inicio da analise sint�tica/sem�ntica\n");
		token = next();
		if(token[0].matches("[Pp]rogram")){
			symbol_table.push("$"); //Dermarca o inicio do escopo
			type_stack.push("$"); //Demarca o inicio do escopo - Para tipos
			token = next();
			if(token[1].matches("Identificador")){
				symbol_table.push(token[0]); //Adiciona o identificador o programa
				type_stack.push("program"); //Identificador do tipo Program, nao pode ser usado no programa
				token = next();
				if(token[0].matches(";")){
					token = next();
					if(variable_dec()){
						if(subprogram_dec()){
							if (compound_cmd()){
								if(token[0].matches("\\."))
									System.out.println("Sintaxe Aceita");
								else
									System.out.println("Erro: Delmitador \'.\' era esperado no final");
							}else{
								System.out.println("Erro: Falha nos comandos compostos na linha " + token[2]);
							}
						}else{
							System.out.println("Erro: Falha nas declara��es de subprogramas na linha " + token[2]);
						}
					}else{
						System.out.println("Erro: Falha na declara��o de vari�veis na linha " + token[2]);
					}
				}else{
					System.out.println("Erro: Delimitador \';\' era esperado na linha " + token[2]);
				}
				
			}else{
				System.out.println("Erro: Identificador era esperado na linha " + token[2]);
			}
			
		}else{
			System.out.println("Erro: Palavra-chave \'Program\' era esperada na linha " + token[2]);
		}
	}
	
	/**------Resgata a pr�xima linha da tabela de tokens------**/
	private String[] next(){
		System.out.println("token lido: " + token_table.get(nLine)[0]);
		return token_table.get(nLine++);
	}
	
	/**------Verifica a existencia do identificador na pilha------**/
	private boolean check_symbol_table(String id){
		//System.out.println(symbol_table);
		if(symbol_table.contains(id)){
			return true; //Retorna true no caso da ocorrencia do id na pilha 
		}else
			return false;
	}
	
	/**------Verifica a existencia do identificador na pilha dentro do escopo------**/
	private boolean check_scope_symbol_table(String id){
		//System.out.println(symbol_table);
		int size = symbol_table.size();
		for(int i = size-1; i >= 0; i--){
			if(symbol_table.get(i).equals("$"))
				break;
			if(symbol_table.get(i).equals(id))
				return true;
		}
		
		return false;
	}
	
	/**------Retorna a ultima posi�ao (na representa�ao de Vector) do identificador correspondente------**/
	private int get_last_index(String id){
		int size = symbol_table.size();
		for(int i = size-1; i >= 0; i--){
			if(symbol_table.get(i).equals(id))
				return i;
		}
		
		return -1;
	}
	
	/**------Retorna o tipo do identificador**/
	private String get_type(String id){
		int index = get_last_index(id);
		return type_stack.get(index);
	}
	
	/**------Adiciona o id do procedure e os seus par�metros formais no hashMap------**/
	private void add_proc_parameter(String id_proc, String id_arg){
		if(!id_proc.equals(""))
			procedures_parameters.get(id_proc).add(id_arg);
	}
	
	/**------Atualiza a lista de identificadores dos argumentos para o tipos dos argumentos------**/
	private void update_proc_parameter(String id_proc){
		ArrayList<String> parameters = procedures_parameters.get(id_proc);
		ArrayList<String> type_parameters = new ArrayList<String>();
		
		for(String parameter : parameters){
			type_parameters.add(get_type(parameter));
		}
		
		procedures_parameters.get(id_proc).clear();
		procedures_parameters.get(id_proc).addAll(type_parameters);
		
	}
	
	/**------Retorna os tipos dos argumentos de um procedure------**/
	private ArrayList<String> get_parameters(String id_proc){
		return procedures_parameters.get(id_proc);
	}
	/**------Verifica os tipos de argumentos de um procedure------**/
	private boolean verify_arguments(String id_proc){
		ArrayList<String> parameters = get_parameters(id_proc);
		System.out.println("Par�metros de " + id_proc + ": " + parameters);
		System.out.println("Argumentos: " + arguments);
		
		int size_p = parameters.size(), size_a = arguments.size();
		
		if(size_p != size_a){
			System.out.println("Erro Sem�ntico: Quantidade de argumentos n�o corresponde � quantidade de par�metros");
			return false;
		}
		
		String param, arg;
		
		for(int i = 0; i < size_p; i++){
			param = parameters.get(i).toLowerCase();
			arg = arguments.get(i).toLowerCase();
			
			if(!param.equals(arg)){
				System.out.println("Erro Sem�ntico: Tipos de argumentos n�o correspondem");
				return false;
			}
		}
		
		return true;
	}
	
	/**------Verifica o tipo de expressao (tipo de retorno) aritmetica e atualiza o pct------**/
	private boolean verify_aritmetic_expression_type(String operation){
		//System.out.println(type_control_stack);
		String op1 = type_control_stack.pop();
		String op2 = type_control_stack.pop();
		
		if(op1.matches("[Pp]rogram") || op2.matches("[Pp]rogram")){
			System.out.println("Erro: N�o � permitido opera��es com o identificador do programa");
			return false;
		}
		
		if(op1.matches("[Ii]nteger") && op2.matches("[Ii]nteger")){
			type_control_stack.push("integer");
			System.out.println("Opera��o: " + op1 + " " + operation + " " + op2 + " - \'OK\'");
			return true;
		}else if(op1.matches("[Ii]nteger") && op2.matches("[Rr]eal")){
			type_control_stack.push("real");
			System.out.println("Opera��o: " + op1 + " " + operation + " " + op2 + " - \'OK\'");
			return true;
		}else if(op1.matches("[Rr]eal") && op2.matches("[Ii]nteger")){
			type_control_stack.push("real");
			System.out.println("Opera��o: " + op1 + " " + operation + " " + op2 + " - \'OK\'");
			return true;
		}else if(op1.matches("[Rr]eal") && op2.matches("[Rr]eal")){
			type_control_stack.push("real");
			System.out.println("Opera��o: " + op1 + " " + operation + " " + op2 + " - \'OK\'");
			return true;
		}else{
			System.out.println("Erro Sem�ntico: Incompatibilidade entre os tipos " + op2 + " e " + op1);
			return false;
		}
	}
	
	/**------Verifica o tipo de expressao (tipo de retorno) relacional e atualiza o pct------**/
	private boolean verify_relational_expression_type(String operation){
		//System.out.println(type_control_stack);
		String op1 = type_control_stack.pop();
		String op2 = type_control_stack.pop();
		
		if(op1.matches("[Pp]rogram") || op2.matches("[Pp]rogram")){
			System.out.println("Erro: N�o � permitido opera��es com o identificador do programa");
			return false;
		}
		
		if(op1.matches("[Ii]nteger") && op2.matches("[Ii]nteger")){
			type_control_stack.push("boolean");
			System.out.println("Opera��o: " + op1 + " " + operation + " " + op2 + " - \'OK\'");
			return true;
		}else if(op1.matches("[Ii]nteger") && op2.matches("[Rr]eal")){
			type_control_stack.push("boolean");
			System.out.println("Opera��o: " + op1 + " " + operation + " " + op2 + " - \'OK\'");
			return true;
		}else if(op1.matches("[Rr]eal") && op2.matches("[Ii]nteger")){
			type_control_stack.push("boolean");
			System.out.println("Opera��o: " + op1 + " " + operation + " " + op2 + " - \'OK\'");
			return true;
		}else if(op1.matches("[Rr]eal") && op2.matches("[Rr]eal")){
			type_control_stack.push("boolean");
			System.out.println("Opera��o: " + op1 + " " + operation + " " + op2 + " - \'OK\'");
			return true;
		}else{
			System.out.println("Erro Sem�ntico: Incompatibilidade entre os tipos " + op2 + " e " + op1);
			return false;
		}
	}
	
	/**------Verifica o tipo de expressao (tipo de retorno) l�gica e atualiza o pct------**/
	private boolean verify_logic_expression(String operation){
		//System.out.println(type_control_stack);
		String op1 = type_control_stack.pop();
		String op2 = type_control_stack.pop();
		
		if(op1.matches("[Pp]rogram") || op2.matches("[Pp]rogram")){
			System.out.println("Erro: N�o � permitido opera��es com o identificador do programa");
			return false;
		}
		
		if(op1.matches("[Bb]oolean") && op2.matches("[Bb]oolean")){
			type_control_stack.push("boolean");
			System.out.println("Opera��o: " + op1 + " " + operation + " " + op2 + " - \'OK\'");
			return true;
		}else{
			System.out.println("Erro Sem�ntico: Tipos incompat�veis na opera��o l�gica");
			return false;
		}
	}
	
	/**------Verifica a compatibilidade de tipos em uma opera�ao de atribui�ao------**/
	private boolean verify_atribution(){
		//System.out.println(type_control_stack);
		String op1 = type_control_stack.pop(); //Tipo da expressao
		String op2 = type_control_stack.pop(); //Tipo da variavel que recebe o valor da expressao
		
		if(op1.matches("[Pp]rogram") || op2.matches("[Pp]rogram")){
			System.out.println("Erro: N�o � permitido opera��es com o identificador do programa");
			return false;
		}
		
		if(op2.matches("[Rr]eal") && (op1.matches("[Ii]nteger") || op1.matches("[Rr]eal"))){
			System.out.println("Opera��o: " + op2 + ":= " + op1 + " - \'OK\'");
			return true;
		}else if(op2.matches("[Ii]nteger") && op1.matches("[Ii]nteger")){
			System.out.println("Opera��o: " + op2 + ":= " + op1 + " - \'OK\'");
			return true;
		}else if(op2.matches("[Bb]oolean") && op1.matches("[Bb]oolean")){
			System.out.println("Opera��o: " + op2 + ":= " + op1 + " - \'OK\'");
			return true;
		}else{
			System.out.println("Erro Sem�ntico: Incompatibilidade entre os tipos " + op2 + " e " + op1);
			return false;
		}
		
	}
	
	/**------Verifica o tipo de retorno da ultima express�o (topo da pilha) computada------**/
	private String check_type_expression(){
		return type_control_stack.pop();
	}
	
	/**------Verifica a opera��o (bin�ria)------**/
	private boolean verify_operation(){
		System.out.println("Tipos dos operandos: " + type_control_stack);
		System.out.println("Opera��es: " + operation_stack);
		
		String operation = operation_stack.pop();
		if(operation.matches("\\+") || operation.matches("\\-") || operation.matches("\\/") || operation.matches("\\*")){//Verifica se � um operador aritmetico
			if(verify_aritmetic_expression_type(operation)){
				System.out.println("Tipo do resultado: " + type_control_stack.peek() + "\n");
				return true;
			}else{
				System.out.println("Erro Sem�ntico: na opera��o " + operation);
				return false;
			}
		}else if(operation.equals("<") || operation.equals(">") || operation.equals("=") || operation.equals("<>") || operation.equals("<=") || operation.equals(">=")){
			if(verify_relational_expression_type(operation)){
				System.out.println("Tipo do resultado: " + type_control_stack.peek() + "\n");
				return true;
			}else{
				System.out.println("Erro Sem�ntico: na opera��o: " + operation);
				return false;
			}
		}else if(operation.matches("[Aa]nd") || operation.matches("[Oo]r") || operation.matches("[Nn]ot")){//Verifica se � um operador l�gico
			if(verify_logic_expression(operation)){
				System.out.println("Tipo do resultado: " + type_control_stack.peek() + "\n");
				return true;
			}else{
				System.out.println("Erro Sem�ntico: na opera��o: " + operation);
				return false;
			}
		}else if(operation.equals(":=")){
			if(verify_atribution()){
				return true;
			}else{
				System.out.println("Erro Sem�ntico: na opera��o: " + operation);
				return false;
			}
		}
		return false;
	}
	
	/**Adiciona na Pct**/
	private void add_pct(String id){
		String id_type = get_type(id);
		System.out.println("tipo de " + token[0] + ": " + id_type);
		type_control_stack.push(id_type); //Pega o tipo da variavel e adiciona no PcT
		System.out.println("Pilha de controle de tipo: " + type_control_stack);
	}	
	
	/**------Verifica e adiciona o identificador na pilha------**/
	private boolean addId_symbol_table(String id){
		if(!check_scope_symbol_table(id)){
			symbol_table.push(id);
			System.out.println("Identificador adicionado na Tabela de s�mbolos\nTabela de Simbolos: " + symbol_table);
			return true;
		}else{
			System.out.println("Erro Sem�ntico: identificador \'" + token[0] + "\' j� foi declarado no escopo");
			return false;
		}
	}
	
	/**------Adiciona o identificador do programa, usado para impedir a declara�ao de vari�veis com o mesmo identificador do programa------**/
	private void push_program_id(){
		String id = symbol_table.get(1); //Indice do identificador do programa
		//System.out.println("id do programa: " + id);
		symbol_table.push(id);
		type_stack.push("program");
	}
	
	/**------Limpa a pilha at� o marcador $------**/
	private void clear_scope(){
		String top = symbol_table.pop();
		type_stack.pop();
		while(!top.matches("\\$")){
			top = symbol_table.pop();
			type_stack.pop();
		}
		
		System.out.println("Tabela de simbolos: " + symbol_table);
		System.out.println("Pilha de tipos: " + type_stack);
	}
	
	/**------Adiciona, n vezes, o tipo na pilha de tipos------**/
	private boolean add_type_stack(String type, int n){
		for(int i = 0; i < n; i++){
			type_stack.push(type);
		}
		
		System.out.println("Pilha de tipos: " + type_stack);
		return true;
	}
	
	
	/**----------------------------------------------------------------------------------------------------------------------------------------------**/
	
	/**------Declara��o de vari�veis------**/
	private boolean variable_dec(){
		System.out.println("\nDeclara��o de vari�veis");
		if(token[0].matches("[Vv]ar")){
			token = next();
			if(var_dec_list()){
				System.out.println("Declara��o de vari�veis - \'Ok\'");
				return true;
			}else
				return false;
		}else{
			return true;
		}
	}
	
	/**----------------------------------------------------------------------------------------------------------------------------------------------**/
	
	/**------Lista de declara��es de vari�veis------**/
	private boolean var_dec_list(){
		
		if(id_list(false)){			
			if(token[0].matches(":")){		
				token = next();
				if(!check_type())
					return false;
				
				if(token[0].matches(";")){

					System.out.println("Lista de declara��es de vari�veis - \'Ok\'\n");
					token = next();
					if(vdl()){
						return true;
					}else{
						return false;
					}
					
				}else{
					System.out.println("Erro: Delimitador \';\' era esperado na linha " + token[2]);
					return false;
				}
			}else{
				System.out.println("Erro: Delimitador \':\' era esperado na linha " + token[2]);
				return false;
			}

		}else{
			return false;
		}
	}
	
	/**---Elimina��o da recursividade a esquerda---**/
	private boolean vdl(){
		if(id_list(false)){			
			if(token[0].matches(":")){		
				token = next();
				if(check_type()){
					if(token[0].matches(";")){
						System.out.println("Lista de declara��es de vari�veis - \'Ok\'\n");
						token = next();
						if(vdl())
							return true;
						else
							return true;
					}else{
						System.out.println("Erro: Delimitador \';\' era esperado na linha " + token[2]);
						return false;
					}
				}else
					return false;
			}else{
				System.out.println("Erro: Delimitador \':\' era esperado na linha " + token[2]);
				return false;
			}
		}else
			return true;
		
	}
	
	/**----------------------------------------------------------------------------------------------------------------------------------------------**/
	
	int countId; //Usado para fazer a contagem de identificadores na lista de identificadores, essa contagem ser� utilizada para atribuir os tipos aos identificadores
	
	/**------Lista de identificadores------**/
	private boolean id_list(boolean argument){ //Argumento utilizado para especificar se a lista de identificadores corresponde aos argumentos de algum procedure
		if(token[1].matches("Identificador")){
			if(!addId_symbol_table(token[0]))
				return false;
			if(argument)
				add_proc_parameter(proc_id, token[0]);
			
			countId = 1;
			token = next();
			if(il(argument)){
				return true;
			}else{
				return false;
			}
			
		}else{
			//System.out.println("Erro: Identificador era esperado na linha " + token[2]);
			return false;
		}
	}
	
	/**---Elimina��o da recursividade a esquerda---**/
	private boolean il(boolean argument){
		if(token[0].matches(",")){
			token = next();
			if(token[1].matches("Identificador")){
				if(!addId_symbol_table(token[0]))
					return false;
				
				if(argument)
					add_proc_parameter(proc_id, token[0]);
				countId++;
				token = next();
				if(il(argument)){
					return true;
				}else{
					return false;
				}
			}else{
				return false;
			}
		}else
			return true;
	}
	/**----------------------------------------------------------------------------------------------------------------------------------------------**/
	
	/**------Tipo------**/
	private boolean check_type(){
		if(token[0].matches("[Ii]nteger") || token[0].matches("[Rr]eal") || token[0].matches("[Bb]oolean")){
			add_type_stack(token[0], countId); //Adiciona o tipo na pilha de tipos count vezes
			token = next();
			return true;
		}else{
			System.out.println("Erro: tipo inv�lido na linha " + token[2]);
			return false;
		}
	}
	
	/**----------------------------------------------------------------------------------------------------------------------------------------------**/
	
	/**------Declara��es de subprogramas------**/
	private boolean subprogram_dec(){
		System.out.println("\nDeclara��es de subprogramas");		
		if(sub_dec()){
			if(token[0].matches(";")){
				token = next();
				if(subprogram_dec()){
					return true;
				}else{
					return false;
				}
			}else{
				System.out.println("Erro: Delimitador \';\' era esperado na linha " + token[2]);
				return false;
			}
		}else{
			return true;
		}
	}
	
	String proc_id = ""; //Usado para guarda o id do procedure atual para relacion�-lo ao seus argumentos
	
	/**------Declara��o de subprograma------**/
	private boolean sub_dec(){
		if(token[0].matches("[Pp]rocedure")){
			token = next();
			if(token[1].matches("Identificador")){
				if(!addId_symbol_table(token[0])) //Adiciona o identificador do procedures
					return false;
				type_stack.push("procedure");
				
				proc_id = token[0];
				procedures_parameters.put(proc_id, new ArrayList<String>());
				
				symbol_table.push("$"); //Novo escopo
				type_stack.push("$");
				
				push_program_id(); //Impede a declara��o de vari�veis com o mesmo identificador do programa
				
				token = next();
				if(arguments()){
					if(token[0].matches(";")){
						token = next();
						if(variable_dec()){
							if(subprogram_dec()){
								if(compound_cmd()){
									return true;
								}else{
									return false; //Falha no comando composto
								}	
							}else{
								return false; //Falha na declara�ao de subprograma
							}
						}else{
							return false; //Falha na declaracao de variaveis
						}
					}else{
						System.out.println("Erro: Delimitador \';\' era esperado na linha " + token[2]);
						return false;
					}
				}else{
					return false;
				}
			}else{
				System.out.println("Erro: Identificador era esperado na linha " + token[2]);
				return false;
			}
		}
		return false;
	}
	
	/**------Argumentos------**/
	private boolean arguments(){
		if(token[0].matches("\\(")){
			token = next();
			if(arguments_list()){
				if(token[0].matches("\\)")){
					token = next();
					update_proc_parameter(proc_id);
					System.out.println("Par�metros de " + proc_id + ": " + procedures_parameters.get(proc_id));
					System.out.println("Argumentos - \'Ok\'");
					return true;
				}else{
					System.out.println("Erro: \')\' era esperado na linha " + token[2]);
					return false;
				}
			}else{
				return false;
			}
			
			
		}else 
			return true;
		
	}
	
	/**------Lista de Argumentos------**/
	private boolean arguments_list(){
		
		if(id_list(true)){
			if(token[0].matches(":")){
				token = next();
				if(check_type()){
					if(token[0].matches(";")){
						token = next();
						if(arguments_list())
							return true;
						else
							return false;
					}else{
						System.out.println("Lista de Argumentos - \'OK\'\n");
						return true;
					}
				}else //Erro de tipo
					return false;
			}else{ //Erro de delimitador
				System.out.println("Erro: Delimitador \':\' era esperado na linha " + token[2]);
				return false;
			}
		}else
			return false;
	}
	
	/**----------------------------------------------------------------------------------------------------------------------------------------------**/
	
	int nivel = 0; //Variavel utilizada para definir o nivel do escopo
	
	/**------Comandos compostos------**/
	private boolean compound_cmd(){
		System.out.println("\nComando composto");
		if(token[0].matches("[Bb]egin")){
			nivel++;
			token = next();
			if(op_cmd()){
				if(token[0].matches("[Ee]nd")){
					nivel--;
					if(nivel ==0)
						clear_scope(); //Destro� as variaveis deste escopo
					token = next();
					System.out.println("Comando composto - OK\n");
					return true;
				}
			}
		}
		return false;
	}
	
	/**------Comandos Opcionais------**/
	private boolean op_cmd(){
		if(cmd_list()){
			return true;
		}else
			return true;
	}
	
	/**----------------------------------------------------------------------------------------------------------------------------------------------**/
	
	/**------Lista de comandos------**/
	private boolean cmd_list(){
		if(command()){
			if(cmdL()){
				System.out.println("Lista de comando - \'OK\'");
				return true;
			}else
				return false;
		}else
			return false;
	}
	
	/**---Elimina��o da recurs�o a esquerda---**/
	private boolean cmdL(){
		if(token[0].matches(";")){
			token = next();
			if(command()){
				if(cmdL())
					return true;
				else
					return false;
			}else{
				return false;
			}
		}else{
			return true;
		}
	}
	
	/**----------------------------------------------------------------------------------------------------------------------------------------------**/
	
	/**------Comando------**/
	private boolean command(){
		if(variable()){
			if(token[1].matches("Operador de Atribui��o")){
				operation_stack.push(token[0]); //Adiciona a opera��o de atribui��o na pilha de opera��es
				token = next();
				if(expression()){
					//Verificar o tipo aqui
					if(!verify_operation())
						return false; //Opera��o inv�lida
					
					System.out.println("Comando - \'OK\'\n");
					return true;
				}else{
					return false;
				}
			}else{
				return false;
			}
		}else if(proc_activate()){
			return true;
		}else if(compound_cmd()){
			return true;
		}else if(token[0].matches("[Ii]f")){
			token = next();
			if(expression()){
				//token = next();
				//Verificar o tipo aqui
				if(!check_type_expression().matches("[Bb]oolean"))
					return false;
				if(token[0].matches("[Tt]hen")){
					token = next();
					if(command()){
						if(else_part()){
							System.out.println("Comando IF - \'OK\'\n");
							return true;
						}else{
							return false;
						}
						
					}else{
						System.out.println("Erro: Comando era esperado na linha " + token[2]);
						return false;
					}
				}else{
					System.out.println("Erro: Palavra chave \'then\' era esperada na linha " + token[2]);
					return false;
				}
			}else{
				System.out.println("Erro: Express�o era esperada na linha " + token[2]);
				return false;
			}
		}else if(token[0].matches("[Ww]hile")){
			token = next();
			if(expression()){
				//token = next();
				//Verificar o tipo aqui
				if(!check_type_expression().matches("[Bb]oolean"))
					return false;
				
				if(token[0].matches("[Dd]o")){
					token = next();
					if(command()){
						System.out.println("Comando While - \'OK\'\n");
						return true;
					}else{
						System.out.println("Erro: Comando era esperado na linha " + token[2]);
						return false;
					}
				}else{
					System.out.println("Erro: Palavra chave \'do\' era esperada na linha " + token[2]);
					return false;
				}
			}else{
				System.out.println("Erro: Express�o era esperada na linha " + token[2]);
				return false;
			}
		}else if(token[0].matches("[Dd]o")){
			token = next();
			if(command()){
				if(token[0].matches("[Ww]hile")){
					token = next();
					if(expression()){
						//Verificar o tipo aqui
						if(!check_type_expression().matches("[Bb]oolean"))
							return false;
						
						return true;
					}else{
						System.out.println("Erro: Express�o era esperada na linha " + token[2]);
						return false;
					}
				}else{
					System.out.println("Erro: Palavra chave While era esperada na linha " + token[2]);
					return false;
				}
			}else{
				System.out.println("Erro: Comando era esperado");
				return false;
			}
		}else
			return false;
	}
	
	/**------Variavel------**/
	private boolean variable(){
		if(token[1].matches("Identificador")){
			if(!check_symbol_table(token[0])){ //Verifica se o identificador foi declarado
				System.out.println("Erro Sem�ntico: identificador \'" + token[0] + "\' n�o foi declarado");
				return false;
			}else{
				if(get_type(token[0]).matches("[Pp]rocedure")) //Verifica se � um identificador de procedure
					return false;
				else
					add_pct(token[0]); //Adiciona na Pct o tipo	da variavel
			}
			token = next();
			return true;
		}else{
			return false;
		}
	}
	
	/**------Parte else------**/
	private boolean else_part(){
		if(token[0].matches("[Ee]lse")){
			token = next();
			if(command()){
				System.out.println("Parte else - \'OK\'\n");
				return true;
			}else{
				System.out.println("Erro: Comando era esperado na linha " + token[2]);
				return false;
			}
		}else
			return true;
	}
	
	private ArrayList<String> arguments; //Array de argumentos, ser� utilizado para a verifica��o de tipo nas ativa�oes de procedimento
	
	/**------Ativa��o de procedimento------**/
	private boolean proc_activate(){
		if(token[1].matches("Identificador")){
			if(!check_symbol_table(token[0])){
				System.out.println("Erro Sem�ntico: identificador \'" + token[0] + "\' n�o foi declarado");
				return false;
			}
			String proc_id = token[0]; //Guarda o identificador do procedure
			token = next();
			if(token[0].matches("\\(")){
				arguments = new ArrayList<String>();
				token = next();
				if(exp_list()){
					//token = next();
					if(token[0].matches("\\)")){
						if(!verify_arguments(proc_id)){
							System.out.println("Erro sem�ntico: Passagem inv�lida de par�metros");
							return false;
						}
						token = next();
						System.out.println("Ativa��o de procedimento - OK\n");
						return true;
					}else{
						System.out.println("Erro: Delimitador \')\' era esperado na linha " + token[2]);
						return false;
					}
				}else{
					System.out.println("Erro: Lista de express�o era esperada na linha " + token[2]);
					return false;
				}
			}else{
				System.out.println("Ativa��o de procedimento - OK");
				return true;
			}
		}else{
			return false;
		}
	}
	
	/**----------------------------------------------------------------------------------------------------------------------------------------------**/
	
	/**------Lista de expressoes------**/
	private boolean exp_list(){
		if(expression()){
			arguments.add(check_type_expression());
			if(exl()){
				return true;
			}else{
				return false;
			}
		}
		return false;
	}
	
	/**---Elimina��o da recursividade a esquerda---**/
	private boolean exl(){
		if(token[0].matches(",")){
			token = next();
			if(expression()){
				arguments.add(check_type_expression());
				if(exl()){
					return true;
				}else{
					return false;
				}
			}else{
				System.out.println("Erro: Express�o era esperada na linha " + token[2]);
				return false;
			}
		}else{
			return true;
		}
	}
	
	/**----------------------------------------------------------------------------------------------------------------------------------------------**/
	
	/**------Express�o------**/
	private boolean expression(){
		if(simple_exp()){
			if(relational_op()){
				if(simple_exp()){
					//Verifica aqui o tipo de expressao!
					if(verify_operation())
						return true;
					else
						return false;
				}else{
					System.out.println("Erro: Express�o era esperada na linha " + token[2]);
					return false;
				}
			}else{
				System.out.println("Express�o - OK\n");
				return true;
			}
		}else{
			System.out.println("Erro: Express�o simples era esperada na linha " + token[2]);
			return false;
		}
	}
	
	/**----------------------------------------------------------------------------------------------------------------------------------------------**/
	
	/**------Express�o Simples------**/
	private boolean simple_exp(){
		if(term()){
			if(s_exp()){
				System.out.println("Express�o Simples - \'OK\'");
				return true;
			}else{
				return false;
			}
		}else if(sign()){
			//token = next();
			if(term()){
				if(s_exp()){
					System.out.println("Express�o Simples - \'OK\'");
					return true;
				}else{
					return false;
				}
			}else{
				System.out.println("Erro: termo era esperado na linha " + token[2]); 
				return false;
			}
		}else
			return false;
	}
	
	/**---Elimina��o de recursividade a esquerda---**/
	private boolean s_exp(){
		if(add_op()){
			if(term()){
				//token = next();
				//Verifica aqui o tipo de expressao!
				if(!verify_operation())
					return false;
				
				if(s_exp()){
					return true;
				}else{
					return false;
				}
			}else{
				System.out.println("Erro: termo era esperado na linha " + token[2]);
				return false;
			}
		}else{
			return true;
		}
	}
	
	/**----------------------------------------------------------------------------------------------------------------------------------------------**/
	
	/**------Termo------**/
	private boolean term(){
		if(factor()){
			if(trm()){
				return true;
			}else{
				return false;
			}
		}else{
			return false;
		}
	}
	
	/**---Elimina��o da recursividade a esquerda---**/
	private boolean trm(){
		if(mult_op()){
			if(factor()){
				//Verifica aqui o tipo de expressao!
				if(!verify_operation())
					return false;
				
				if(trm()){
					return true;
				}else{
					return false;
				}
			}else
				return false;
		}else
			return true;
	}
	
	/**----------------------------------------------------------------------------------------------------------------------------------------------**/
	
	/**------Fator------**/
	private boolean factor(){
		
		if(token[1].matches("Identificador")){
			if(!check_symbol_table(token[0])){
				System.out.println("Erro Sem�ntico: identificador \'" + token[0] + "\' n�o foi declarado");
				return false;
			}else
				add_pct(token[0]); //Adiciona o tipo de identificador no pct
			token = next();
			if(token[0].matches("\\(")){
				token = next();
				if(exp_list()){
					if(token[0].matches("\\)")){
						token = next();
						System.out.println("Fator - \'OK\'");
						return true;
					}else{
						System.out.println("Erro: Delimitador \')\' era esperado na linha " + token[2]);
						return false;
					}
				}else{
					System.out.println("Erro: Lista de express�es era esperado na linha " + token[2]);
					return false;
				}
			}else{
				return true;
			}
			
		}else if(token[1].matches("N�mero Inteiro")){
			System.out.println("Fator - \'OK\'");
			type_control_stack.push("integer");
			token = next();
			return true;
			
		}else if(token[1].matches("N�mero Real")){
			System.out.println("Fator - \'OK\'");
			type_control_stack.push("real");
			token = next();
			return true;
			
		}else if(token[0].matches("[Tt]rue")){
			System.out.println("Fator - \'OK\'");
			type_control_stack.push("boolean");
			token = next();
			return true;
			
		}else if(token[0].matches("[Ff]alse")){
			System.out.println("Fator - \'OK\'");
			type_control_stack.push("boolean");
			token = next();
			return true;
			
		}else if(token[0].matches("\\(")){
			token = next();
			if(expression()){
				if(token[0].matches("\\)")){
					token = next();
					System.out.println("Fator - \'OK\'");
					return true;
				}else{
					System.out.println("Erro: Delimitador \')\' era esperado na linha " + token[2]);
					return false;
				}
			}else{
				System.out.println("Erro: Express�o era esperado na linha " + token[2]);
				return false;
			}
		}else if(token[0].matches("[Nn]ot")){
			token = next();
			if(!get_type(token[0]).equals("boolean")){
				System.out.println("Erro Sem�ntico: Opera��o NOT inv�lida");
				return false;
			}
			if(factor()){
				System.out.println("Fator - \'OK\'");
				return true;
			}else{
				return false;
			}
		}
		return false;
	}
	
	/**------Sinal------**/
	private boolean sign(){
		if(token[0].matches("\\+") || token[0].matches("\\-")){
			System.out.println("Sinal - OK");
			token = next();
			return true;
		}else{
			return false;
		}
	}
	
	/**------Operador Relacional------**/
	private boolean relational_op(){
		if(token[1].matches("Operador Relacional")){
			System.out.println("Operador Relacional - OK");
			operation_stack.push(token[0]);
			token = next();
			return true;
		}else{
			return false;
		}
	}
	
	/**------Operador Aditivo------**/
	private boolean add_op(){
		if(token[0].matches("\\+") || token[0].matches("\\-") || token[0].matches("[Oo]r")){
			System.out.println("Operador Aditivo - OK");
			operation_stack.push(token[0]);
			token = next();
			return true;
		}else{
			return false;
		}
	}
	
	/**------Operador Multiplicativo------**/
	private boolean mult_op(){
		if(token[0].matches("\\*") || token[0].matches("\\/") || token[0].matches("[Aa]nd")){
			System.out.println("Operador Multiplicativo - OK");
			operation_stack.push(token[0]);
			token = next();
			return true;
		}else{
			return false;
		}
	}
	
}
