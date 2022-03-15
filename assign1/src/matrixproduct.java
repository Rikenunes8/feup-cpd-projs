import java.util.Scanner;

import static java.lang.Math.min;

public class matrixproduct {
    public static void main(String[] args) {
        int lin, col, blockSize;
        int op;
        do {
            Scanner myObj = new Scanner(System.in);  // Create a Scanner object

            System.out.println("\n0. Exit");
            System.out.println("1. Multiplication");
            System.out.println("2. Line Multiplication");
            System.out.println("3. Block Multiplication");
            System.out.println("Selection?: ");

            op = myObj.nextInt();  // Read user input
            if (op == 0)
                break;
            System.out.println("Dimensions: lins=cols ? ");
            lin = myObj.nextInt();  // Read user input
            col = lin;


            // Start counting
            switch (op) {
                case 1:
                    OnMult(lin, col);
                    break;
                case 2:
                    OnMultLine(lin, col);
                    break;
                case 3:
                    System.out.println("Block Size? ");
                    blockSize = myObj.nextInt();  // Read user input
                    OnMultBlock(lin, col, blockSize);
                    break;
            }
        }while (op != 0);
    }

    static void OnMult(int m_ar, int m_br) {
        double temp;
        int i, j, k;

        double[] pha = new double[m_ar*m_ar];
        double[] phb = new double[m_ar*m_ar];
        double[] phc = new double[m_ar*m_ar];


        for(i=0; i<m_ar; i++) {
            for(j=0; j<m_ar; j++) {
                pha[i*m_ar + j] = 1.0;
            }
        }
        for(i=0; i<m_br; i++) {
            for(j=0; j<m_br; j++) {
                phb[i*m_br + j] = i+1;
            }
        }
        for(i=0; i<m_br; i++) {
            for(j=0; j<m_br; j++) {
                phc[i*m_br + j] = 0;
            }
        }

        long Time1 = System.currentTimeMillis();
        for(i=0; i<m_ar; i++) {
            for( j=0; j<m_br; j++) {
                temp = 0;
                for( k=0; k<m_ar; k++) {
                    temp += pha[i*m_ar+k] * phb[k*m_br+j];
                }
                phc[i*m_ar+j]=temp;
            }
        }
        long Time2 = System.currentTimeMillis();

        System.out.printf("Time: %3.3f seconds%n", (double)(Time2 - Time1) / 1000);

        // display 10 elements of the result matrix tto verify correctness
        System.out.println("Result matrix: ");
        for(i=0; i<1; i++) {
            for(j=0; j<min(10,m_br); j++) {
                System.out.print(phc[j] + " ");
            }
        }
        System.out.println();
    }

    // add code here for line x line matriz multiplication
    static void OnMultLine(int m_ar, int m_br) {
        double temp;
        int i, j, k;

        double[] pha = new double[m_ar*m_ar];
        double[] phb = new double[m_ar*m_ar];
        double[] phc = new double[m_ar*m_ar];


        for(i=0; i<m_ar; i++) {
            for(j=0; j<m_ar; j++) {
                pha[i*m_ar + j] = 1.0;
            }
        }
        for(i=0; i<m_br; i++) {
            for(j=0; j<m_br; j++) {
                phb[i*m_br + j] = i+1;
            }
        }
        for(i=0; i<m_br; i++) {
            for(j=0; j<m_br; j++) {
                phc[i*m_br + j] = 0;
            }
        }

        long Time1 = System.currentTimeMillis();
        for(i=0; i<m_ar; i++) {
            for( k=0; k<m_ar; k++) {
                for( j=0; j<m_br; j++) {
                    phc[i*m_ar+j] += pha[i*m_ar+k] * phb[k*m_br+j];
                }
            }
        }
        long Time2 = System.currentTimeMillis();

        System.out.printf("Time: %3.3f seconds%n", (double)(Time2 - Time1) / 1000);

        // display 10 elements of the result matrix tto verify correctness
        System.out.println("Result matrix: ");
        for(i=0; i<1; i++) {
            for(j=0; j<min(10,m_br); j++) {
                System.out.print(phc[j] + " ");
            }
        }
        System.out.println();

    }

    // add code here for block x block matriz multiplication
    public static void OnMultBlock(int m_ar, int m_br, int bkSize) {


    }
}
