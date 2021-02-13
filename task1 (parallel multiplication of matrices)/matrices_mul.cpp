#include <cstdio>
#include <cstdlib>
#include <mpi.h>

void printMatrix(double* matrix, int rows, int columns) {
    for (int i = 0; i < rows; i++) {
        for (int j = 0; j < columns; j++) {
            printf("%lf ", matrix[i * rows + j]);
        }
        printf("\n");
    }
}

void fillMatrix(double* matrix, int rows, int columns) {
    for (int i = 0; i < rows; i++) {
        for (int j = 0; j < columns; j++) {
            scanf("%lf", &matrix[i * rows + j]);
        }
    }
}

void multiplyMatrices(double* matrix1, double* matrix2, double* mul, int size1, int size2, int size3, int gridSizeX, int gridSizeY) {
    // matrix1 = size1 * size2
    // matrix2 = size2 * size3
    // mul = size1 * size3
    // mul = matrix1 * matrix2

    // the parallel execution of algorithm is carried out on a 2D-grid of computers of size p1 x p2
    // matrix1 is cut into p1 horizontal strips
    // matrix2 is cut into p2 vertical strips
    // result matrix mul is cut into p1 x p2 sub-matrices

    // creating new communicator for grid
    MPI_Comm grid;
    int p[2] = {gridSizeX, gridSizeY};
    int* periods = (int*) calloc(2, sizeof(int));
    MPI_Cart_create(MPI_COMM_WORLD, 2, p, periods, 0, &grid);

    // getting rank & coordinates of current process
    int grid_rank, grid_coords[2];
    MPI_Comm_rank(grid, &grid_rank);
    MPI_Cart_coords(grid, grid_rank, 2, grid_coords);

    // creating communicators for sub-grids
    MPI_Comm sub_grids[2];
    int** remain_dims = (int**) malloc(sizeof(int*) * 2);
    remain_dims[0] = (int*) calloc(2, sizeof(int));
    remain_dims[0][1] = 1;
    remain_dims[1] = (int*) calloc(2, sizeof(int));
    remain_dims[1][0] = 1;
    // remain_dims = {{0, 1}, {1, 0}}
    MPI_Cart_sub(grid, remain_dims[1], &sub_grids[1]);
    MPI_Cart_sub(grid, remain_dims[0], &sub_grids[0]);

    // getting sizes of stripes in mat1 & mat2 + sub-matrices of result matrix
    int sub_sizes[2] = {size1 / p[0], size3 / p[1]};

    double *sub_mat1 = (double*) malloc(sizeof(double) * sub_sizes[0] * size2);
    // sub_mat1 == local sub-matrix of mat1, including gridSizeX horizontal stripes
    double *sub_mat2 = (double*) malloc(sizeof(double) * size2 * sub_sizes[1]);
    // sub_mat2 == local sub-matrix of mat2, including gridSizeY vertical stripes
    double *sub_mul = (double*) malloc(sizeof(double) * sub_sizes[0] * sub_sizes[1]);
    // sub_mul == local sub-matrix of multiplication matrix

    // counts & displacements
    int *count_2, *displs_2, *count_mul, *displs_mul, *block_lengths;

    // array of data-types
    MPI_Datatype types[3] {MPI_DOUBLE, MPI_UB};

    if (grid_rank == 0) {
        // displacements
        MPI_Aint displs[2]{0, (MPI_Aint) (sizeof(double) * sub_sizes[1])};

        // init
        displs_2 = (int*) malloc(sizeof(int) * p[1]);
        count_2 = (int*) malloc(sizeof(int) * p[1]);
        displs_mul = (int*) malloc(sizeof(int) * p[0] * p[1]);
        count_mul = (int*) malloc(sizeof(int) * p[0] * p[1]);
        for (int i = 0; i < p[0]; i++) {
            for (int j = 0; j < p[1]; j++) {
                if (i == 0) {
                    // executes once
                    displs_2[j] = j;
                    count_2[j] = 1;
                }
                displs_mul[i * p[1] + j] = i * p[1] * sub_sizes[0] + j;
                count_mul[i * p[1] + j] = 1;
            }
        }

        // creating new data-type (types[2])
        MPI_Type_vector(size2, sub_sizes[1], size3, MPI_DOUBLE, &types[0]);
        block_lengths = (int*) malloc(sizeof(int) * 2);
        block_lengths[0] = block_lengths[1] = 1;
        MPI_Type_struct(2, block_lengths, displs, types, &types[2]);
        MPI_Type_commit(&types[2]);
    }

    if (!grid_coords[0]) {
        // main process sends horizontal mat2-stripes in y-coordinate
        MPI_Scatterv(matrix2, count_2, displs_2, types[2], sub_mat2, size2 * sub_sizes[1], MPI_DOUBLE, 0, sub_grids[0]);
    }

    if (!grid_coords[1]) {
        // main process sends horizontal mat1-stripes in x-coordinate
        MPI_Scatter(matrix1, sub_sizes[0] * size2, MPI_DOUBLE, sub_mat1, sub_sizes[0] * size2, MPI_DOUBLE, 0, sub_grids[1]);
    }

    // sending sub-matrices of mat1 in y dimension
    MPI_Bcast(sub_mat1, sub_sizes[0] *  size2, MPI_DOUBLE, 0, sub_grids[0]);
    // sending sub-matrices of mat2 in x dimension
    MPI_Bcast(sub_mat2, size2 * sub_sizes[1], MPI_DOUBLE, 0, sub_grids[1]);

    // counting sub-matrices of result matrix in every process
    for (int i = 0; i < sub_sizes[0]; i++) {
        for (int j = 0; j < sub_sizes[1]; j++) {
            sub_mul[sub_sizes[1] * i + j] = 0;
            for (int k = 0; k < size2; k++) {
                sub_mul[sub_sizes[1] * i + j] += sub_mat1[size2 * i + k] * sub_mat2[sub_sizes[1] * k + j];
            }
        }
    }

    // gathering sub-matrices of multiplication in main process
    MPI_Gatherv(sub_mul, sub_sizes[0]*sub_sizes[1], MPI_DOUBLE, mul, count_mul, displs_mul, types[2], 0, grid);

    MPI_Comm_free(&grid);
    MPI_Comm_free(&sub_grids[0]);
    MPI_Comm_free(&sub_grids[1]);

    free(periods);
    for (int i = 0; i < 2; i++) {
        free(remain_dims[i]);
    }
    free(remain_dims);
    free(sub_mat1);
    free(sub_mat2);
    free(sub_mul);

    if (grid_rank == 0) {
        MPI_Type_free(&types[0]);
        MPI_Type_free(&types[2]);
        free(displs_2);
        free(count_2);
        free(displs_mul);
        free(count_mul);
        free(block_lengths);
    }
}

int main(int argc, char **argv) {
    // argv[1], argv[2], argv[3] == matrices sizes
    // argv[4], argv[5] == 2D-grid sizes

    MPI_Init(&argc, &argv); // initializing MPI

    int rank;
    // every process gets its rank
    MPI_Comm_rank(MPI_COMM_WORLD, &rank);

    int size1 = atoi(argv[1]);
    int size2 = atoi(argv[2]);
    int size3 = atoi(argv[3]);
    int gridSizeX = atoi(argv[4]);
    int gridSizeY = atoi(argv[5]);

    double *A, *B, *C;

    if (rank == 0) {
        A = (double*) malloc(size1 * size2 * sizeof(double));
        B = (double*) malloc(size2 * size3 * sizeof(double));
        C = (double*) malloc(size1 * size3 * sizeof(double));

        printf("INPUT MATRIX A:\n");
        fillMatrix(A, size1, size2);

        printf("INPUT MATRIX B:\n");
        fillMatrix(B, size2, size3);

        printf("MATRIX A:\n");
        printMatrix(A, size1, size2);

        printf("MATRIX B:\n");
        printMatrix(B, size2, size3);
    }

    multiplyMatrices(A, B, C, size1, size2, size3, gridSizeX, gridSizeY);

    if (rank == 0) {
        printf("MATRIX C:\n");
        printMatrix(C, size1, size3);

        free(A);
        free(B);
        free(C);
    }

    MPI_Finalize();

    return 0;
}