import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import javax.swing.JOptionPane;
import java.util.ArrayList;

public class Main {

	public static void main(String[] args) {
		
		try{
			ArrayList <String[]> lexic_table;
			String dir = JOptionPane.showInputDialog(null, "Digite o diretorio do arquivo");
			FileReader file = new FileReader(dir); //Localizacao do arquivo
			BufferedReader arq = new BufferedReader(file);
			
			AnalisadorLexico al = new AnalisadorLexico(arq);
			al.analisar();
			al.show_table();
			lexic_table = al.return_table();
			
			arq.close();
			file.close();
			
			AnalisadorSintatico as = new AnalisadorSintatico(lexic_table);
			as.analisar();
			
		}catch(FileNotFoundException fnf){
			System.out.println("Arquivo nao encontrado");
		}catch(IOException ioe){
			System.out.println("Erro");
		}catch(Exception e){
			e.printStackTrace();
		}

	}

}
