import java.util.ArrayList;
import java.util.Stack;
import java.util.HashMap;
import java.util.ArrayList;

public class AnalisadorSintatico {
	
	private ArrayList<String[]> token_table; //Tabela de token retornada pelo analisador lexico
	private Stack<String> symbol_table; //Pilha para implementar a tabela de simbolos
	private Stack<String> type_stack; //Pilha para implementar a tabela de tipos, a uniao entre symbol_table e type_table funcionaria como um HashMap, onde o indice mapeia os elementos das duas pilhas
	private Stack<String> type_control_stack; //Pilha de controle de tipo (PcT), será usada nos comandos de atribuiçao, expressao aritmetica, relacional e logica
	private Stack<String> operation_stack; //Pilha para armazenar as operaçoes, forma que utilizei para solucionar a verificaçao de tipos
	private HashMap<String, ArrayList<String>> procedures_parameters; //Usado para fazer a checagem de tipos na ativação de procedimentos
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
		System.out.println("Inicio da analise sintática/semântica\n");
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
							System.out.println("Erro: Falha nas declarações de subprogramas na linha " + token[2]);
						}
					}else{
						System.out.println("Erro: Falha na declaração de variáveis na linha " + token[2]);
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
	
	/**------Resgata a próxima linha da tabela de tokens------**/
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
	
	/**------Retorna a ultima posiçao (na representaçao de Vector) do identificador correspondente------**/
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
	
	/**------Adiciona o id do procedure e os seus parâmetros formais no hashMap------**/
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
		System.out.println("Parâmetros de " + id_proc + ": " + parameters);
		System.out.println("Argumentos: " + arguments);
		
		int size_p = parameters.size(), size_a = arguments.size();
		
		if(size_p != size_a){
			System.out.println("Erro Semântico: Quantidade de argumentos não corresponde à quantidade de parâmetros");
			return false;
		}
		
		String param, arg;
		
		for(int i = 0; i < size_p; i++){
			param = parameters.get(i).toLowerCase();
			arg = arguments.get(i).toLowerCase();
			
			if(!param.equals(arg)){
				System.out.println("Erro Semântico: Tipos de argumentos não correspondem");
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
			System.out.println("Erro: Não é permitido operações com o identificador do programa");
			return false;
		}
		
		if(op1.matches("[Ii]nteger") && op2.matches("[Ii]nteger")){
			type_control_stack.push("integer");
			System.out.println("Operação: " + op1 + " " + operation + " " + op2 + " - \'OK\'");
			return true;
		}else if(op1.matches("[Ii]nteger") && op2.matches("[Rr]eal")){
			type_control_stack.push("real");
			System.out.println("Operação: " + op1 + " " + operation + " " + op2 + " - \'OK\'");
			return true;
		}else if(op1.matches("[Rr]eal") && op2.matches("[Ii]nteger")){
			type_control_stack.push("real");
			System.out.println("Operação: " + op1 + " " + operation + " " + op2 + " - \'OK\'");
			return true;
		}else if(op1.matches("[Rr]eal") && op2.matches("[Rr]eal")){
			type_control_stack.push("real");
			System.out.println("Operação: " + op1 + " " + operation + " " + op2 + " - \'OK\'");
			return true;
		}else{
			System.out.println("Erro Semântico: Incompatibilidade entre os tipos " + op2 + " e " + op1);
			return false;
		}
	}
	
	/**------Verifica o tipo de expressao (tipo de retorno) relacional e atualiza o pct------**/
	private boolean verify_relational_expression_type(String operation){
		//System.out.println(type_control_stack);
		String op1 = type_control_stack.pop();
		String op2 = type_control_stack.pop();
		
		if(op1.matches("[Pp]rogram") || op2.matches("[Pp]rogram")){
			System.out.println("Erro: Não é permitido operações com o identificador do programa");
			return false;
		}
		
		if(op1.matches("[Ii]nteger") && op2.matches("[Ii]nteger")){
			type_control_stack.push("boolean");
			System.out.println("Operação: " + op1 + " " + operation + " " + op2 + " - \'OK\'");
			return true;
		}else if(op1.matches("[Ii]nteger") && op2.matches("[Rr]eal")){
			type_control_stack.push("boolean");
			System.out.println("Operação: " + op1 + " " + operation + " " + op2 + " - \'OK\'");
			return true;
		}else if(op1.matches("[Rr]eal") && op2.matches("[Ii]nteger")){
			type_control_stack.push("boolean");
			System.out.println("Operação: " + op1 + " " + operation + " " + op2 + " - \'OK\'");
			return true;
		}else if(op1.matches("[Rr]eal") && op2.matches("[Rr]eal")){
			type_control_stack.push("boolean");
			System.out.println("Operação: " + op1 + " " + operation + " " + op2 + " - \'OK\'");
			return true;
		}else{
			System.out.println("Erro Semântico: Incompatibilidade entre os tipos " + op2 + " e " + op1);
			return false;
		}
	}
	
	/**------Verifica o tipo de expressao (tipo de retorno) lógica e atualiza o pct------**/
	private boolean verify_logic_expression(String operation){
		//System.out.println(type_control_stack);
		String op1 = type_control_stack.pop();
		String op2 = type_control_stack.pop();
		
		if(op1.matches("[Pp]rogram") || op2.matches("[Pp]rogram")){
			System.out.println("Erro: Não é permitido operações com o identificador do programa");
			return false;
		}
		
		if(op1.matches("[Bb]oolean") && op2.matches("[Bb]oolean")){
			type_control_stack.push("boolean");
			System.out.println("Operação: " + op1 + " " + operation + " " + op2 + " - \'OK\'");
			return true;
		}else{
			System.out.println("Erro Semântico: Tipos incompatíveis na operação lógica");
			return false;
		}
	}
	
	/**------Verifica a compatibilidade de tipos em uma operaçao de atribuiçao------**/
	private boolean verify_atribution(){
		//System.out.println(type_control_stack);
		String op1 = type_control_stack.pop(); //Tipo da expressao
		String op2 = type_control_stack.pop(); //Tipo da variavel que recebe o valor da expressao
		
		if(op1.matches("[Pp]rogram") || op2.matches("[Pp]rogram")){
			System.out.println("Erro: Não é permitido operações com o identificador do programa");
			return false;
		}
		
		if(op2.matches("[Rr]eal") && (op1.matches("[Ii]nteger") || op1.matches("[Rr]eal"))){
			System.out.println("Operação: " + op2 + ":= " + op1 + " - \'OK\'");
			return true;
		}else if(op2.matches("[Ii]nteger") && op1.matches("[Ii]nteger")){
			System.out.println("Operação: " + op2 + ":= " + op1 + " - \'OK\'");
			return true;
		}else if(op2.matches("[Bb]oolean") && op1.matches("[Bb]oolean")){
			System.out.println("Operação: " + op2 + ":= " + op1 + " - \'OK\'");
			return true;
		}else{
			System.out.println("Erro Semântico: Incompatibilidade entre os tipos " + op2 + " e " + op1);
			return false;
		}
		
	}
	
	/**------Verifica o tipo de retorno da ultima expressão (topo da pilha) computada------**/
	private String check_type_expression(){
		return type_control_stack.pop();
	}
	
	/**------Verifica a operação (binária)------**/
	private boolean verify_operation(){
		System.out.println("Tipos dos operandos: " + type_control_stack);
		System.out.println("Operações: " + operation_stack);
		
		String operation = operation_stack.pop();
		if(operation.matches("\\+") || operation.matches("\\-") || operation.matches("\\/") || operation.matches("\\*")){//Verifica se é um operador aritmetico
			if(verify_aritmetic_expression_type(operation)){
				System.out.println("Tipo do resultado: " + type_control_stack.peek() + "\n");
				return true;
			}else{
				System.out.println("Erro Semântico: na operação " + operation);
				return false;
			}
		}else if(operation.equals("<") || operation.equals(">") || operation.equals("=") || operation.equals("<>") || operation.equals("<=") || operation.equals(">=")){
			if(verify_relational_expression_type(operation)){
				System.out.println("Tipo do resultado: " + type_control_stack.peek() + "\n");
				return true;
			}else{
				System.out.println("Erro Semântico: na operação: " + operation);
				return false;
			}
		}else if(operation.matches("[Aa]nd") || operation.matches("[Oo]r") || operation.matches("[Nn]ot")){//Verifica se é um operador lógico
			if(verify_logic_expression(operation)){
				System.out.println("Tipo do resultado: " + type_control_stack.peek() + "\n");
				return true;
			}else{
				System.out.println("Erro Semântico: na operação: " + operation);
				return false;
			}
		}else if(operation.equals(":=")){
			if(verify_atribution()){
				return true;
			}else{
				System.out.println("Erro Semântico: na operação: " + operation);
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
			System.out.println("Identificador adicionado na Tabela de símbolos\nTabela de Simbolos: " + symbol_table);
			return true;
		}else{
			System.out.println("Erro Semântico: identificador \'" + token[0] + "\' já foi declarado no escopo");
			return false;
		}
	}
	
	/**------Adiciona o identificador do programa, usado para impedir a declaraçao de variáveis com o mesmo identificador do programa------**/
	private void push_program_id(){
		String id = symbol_table.get(1); //Indice do identificador do programa
		//System.out.println("id do programa: " + id);
		symbol_table.push(id);
		type_stack.push("program");
	}
	
	/**------Limpa a pilha até o marcador $------**/
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
	
	/**------Declaração de variáveis------**/
	private boolean variable_dec(){
		System.out.println("\nDeclaração de variáveis");
		if(token[0].matches("[Vv]ar")){
			token = next();
			if(var_dec_list()){
				System.out.println("Declaração de variáveis - \'Ok\'");
				return true;
			}else
				return false;
		}else{
			return true;
		}
	}
	
	/**----------------------------------------------------------------------------------------------------------------------------------------------**/
	
	/**------Lista de declarações de variáveis------**/
	private boolean var_dec_list(){
		
		if(id_list(false)){			
			if(token[0].matches(":")){		
				token = next();
				if(!check_type())
					return false;
				
				if(token[0].matches(";")){

					System.out.println("Lista de declarações de variáveis - \'Ok\'\n");
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
	
	/**---Eliminação da recursividade a esquerda---**/
	private boolean vdl(){
		if(id_list(false)){			
			if(token[0].matches(":")){		
				token = next();
				if(check_type()){
					if(token[0].matches(";")){
						System.out.println("Lista de declarações de variáveis - \'Ok\'\n");
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
	
	int countId; //Usado para fazer a contagem de identificadores na lista de identificadores, essa contagem será utilizada para atribuir os tipos aos identificadores
	
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
	
	/**---Eliminação da recursividade a esquerda---**/
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
			System.out.println("Erro: tipo inválido na linha " + token[2]);
			return false;
		}
	}
	
	/**----------------------------------------------------------------------------------------------------------------------------------------------**/
	
	/**------Declarações de subprogramas------**/
	private boolean subprogram_dec(){
		System.out.println("\nDeclarações de subprogramas");		
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
	
	String proc_id = ""; //Usado para guarda o id do procedure atual para relacioná-lo ao seus argumentos
	
	/**------Declaração de subprograma------**/
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
				
				push_program_id(); //Impede a declaração de variáveis com o mesmo identificador do programa
				
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
								return false; //Falha na declaraçao de subprograma
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
					System.out.println("Parâmetros de " + proc_id + ": " + procedures_parameters.get(proc_id));
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
						clear_scope(); //Destroí as variaveis deste escopo
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
	
	/**---Eliminação da recursão a esquerda---**/
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
			if(token[1].matches("Operador de Atribuição")){
				operation_stack.push(token[0]); //Adiciona a operação de atribuição na pilha de operações
				token = next();
				if(expression()){
					//Verificar o tipo aqui
					if(!verify_operation())
						return false; //Operação inválida
					
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
				System.out.println("Erro: Expressão era esperada na linha " + token[2]);
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
				System.out.println("Erro: Expressão era esperada na linha " + token[2]);
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
						System.out.println("Erro: Expressão era esperada na linha " + token[2]);
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
				System.out.println("Erro Semântico: identificador \'" + token[0] + "\' não foi declarado");
				return false;
			}else{
				if(get_type(token[0]).matches("[Pp]rocedure")) //Verifica se é um identificador de procedure
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
	
	private ArrayList<String> arguments; //Array de argumentos, será utilizado para a verificação de tipo nas ativaçoes de procedimento
	
	/**------Ativação de procedimento------**/
	private boolean proc_activate(){
		if(token[1].matches("Identificador")){
			if(!check_symbol_table(token[0])){
				System.out.println("Erro Semântico: identificador \'" + token[0] + "\' não foi declarado");
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
							System.out.println("Erro semântico: Passagem inválida de parâmetros");
							return false;
						}
						token = next();
						System.out.println("Ativação de procedimento - OK\n");
						return true;
					}else{
						System.out.println("Erro: Delimitador \')\' era esperado na linha " + token[2]);
						return false;
					}
				}else{
					System.out.println("Erro: Lista de expressão era esperada na linha " + token[2]);
					return false;
				}
			}else{
				System.out.println("Ativação de procedimento - OK");
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
	
	/**---Eliminação da recursividade a esquerda---**/
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
				System.out.println("Erro: Expressão era esperada na linha " + token[2]);
				return false;
			}
		}else{
			return true;
		}
	}
	
	/**----------------------------------------------------------------------------------------------------------------------------------------------**/
	
	/**------Expressão------**/
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
					System.out.println("Erro: Expressão era esperada na linha " + token[2]);
					return false;
				}
			}else{
				System.out.println("Expressão - OK\n");
				return true;
			}
		}else{
			System.out.println("Erro: Expressão simples era esperada na linha " + token[2]);
			return false;
		}
	}
	
	/**----------------------------------------------------------------------------------------------------------------------------------------------**/
	
	/**------Expressão Simples------**/
	private boolean simple_exp(){
		if(term()){
			if(s_exp()){
				System.out.println("Expressão Simples - \'OK\'");
				return true;
			}else{
				return false;
			}
		}else if(sign()){
			//token = next();
			if(term()){
				if(s_exp()){
					System.out.println("Expressão Simples - \'OK\'");
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
	
	/**---Eliminação de recursividade a esquerda---**/
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
	
	/**---Eliminação da recursividade a esquerda---**/
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
				System.out.println("Erro Semântico: identificador \'" + token[0] + "\' não foi declarado");
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
					System.out.println("Erro: Lista de expressões era esperado na linha " + token[2]);
					return false;
				}
			}else{
				return true;
			}
			
		}else if(token[1].matches("Número Inteiro")){
			System.out.println("Fator - \'OK\'");
			type_control_stack.push("integer");
			token = next();
			return true;
			
		}else if(token[1].matches("Número Real")){
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
				System.out.println("Erro: Expressão era esperado na linha " + token[2]);
				return false;
			}
		}else if(token[0].matches("[Nn]ot")){
			token = next();
			if(!get_type(token[0]).equals("boolean")){
				System.out.println("Erro Semântico: Operação NOT inválida");
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
