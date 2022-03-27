import java.util.Scanner;

import static java.lang.Math.min;

public class matrixproduct {
    public static void main(String[] args) {
        int n, blockSize;
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
            n = myObj.nextInt();  // Read user input

            // Start counting
            switch (op) {
                case 1:
                    OnMult(n);
                    break;
                case 2:
                    OnMultLine(n);
                    break;
                case 3:
                    System.out.println("Block Size? ");
                    blockSize = myObj.nextInt();  // Read user input
                    OnMultBlock(n, blockSize);
                    break;
            }
        }while (op != 0);
    }

    static void OnMult(int n) {
        double temp;
        int i, j, k;

        double[] pha = new double[n*n];
        double[] phb = new double[n*n];
        double[] phc = new double[n*n];


        for(i=0; i<n; i++) {
            for(j=0; j<n; j++) {
                pha[i*n + j] = 1.0;
                phb[i*n + j] = i+1;
                phc[i*n + j] = 0;
            }
        }

        long Time1 = System.currentTimeMillis();
        for(i=0; i<n; i++) {
            for( j=0; j<n; j++) {
                for( k=0; k<n; k++) {
                    phc[i*n+j] += pha[i*n+k] * phb[k*n+j];
                }
            }
        }
        long Time2 = System.currentTimeMillis();

        System.out.printf("Time: %3.3f seconds%n", (double)(Time2 - Time1) / 1000);

        // display 10 elements of the result matrix tto verify correctness
        System.out.println("Result matrix: ");
        for(i=0; i<1; i++) {
            for(j=0; j<min(10,n); j++) {
                System.out.print(phc[j] + " ");
            }
        }
        System.out.println();
    }

    static void OnMultLine(int n) {
        double temp;
        int i, j, k;

        double[] pha = new double[n*n];
        double[] phb = new double[n*n];
        double[] phc = new double[n*n];


        for(i=0; i<n; i++) {
            for(j=0; j<n; j++) {
                pha[i*n + j] = 1.0;
                phb[i*n + j] = i+1;
                phc[i*n + j] = 0;
            }
        }

        long Time1 = System.currentTimeMillis();
        for(i=0; i<n; i++) {
            for( k=0; k<n; k++) {
                for( j=0; j<n; j++) {
                    phc[i*n+j] += pha[i*n+k] * phb[k*n+j];
                }
            }
        }
        long Time2 = System.currentTimeMillis();

        System.out.printf("Time: %3.3f seconds%n", (double)(Time2 - Time1) / 1000);

        // display 10 elements of the result matrix tto verify correctness
        System.out.println("Result matrix: ");
        for(i=0; i<1; i++) {
            for(j=0; j<min(10,n); j++) {
                System.out.print(phc[j] + " ");
            }
        }
        System.out.println();

    }

    // Not asked to be implemented in the project specification
    public static void OnMultBlock(int n, int bkSize) {


    }
}
