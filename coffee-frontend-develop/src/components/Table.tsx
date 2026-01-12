import {
    CircularProgress,
    Table as MuiTable,
    Paper,
    Stack,
    TableBody,
    TableCell,
    TableContainer,
    TableFooter,
    TableHead,
    TableRow,
} from '@mui/material';

import noData from '@assets/noData.svg';

type TableProps<T> = {
    headData: string[];
    bodyData: T[] | undefined;
    renderRow: (row: T) => React.ReactNode;
    tableFooter?: React.ReactNode;
    isLoading?: boolean;
};

const Table = <T,>({ headData, bodyData, renderRow, tableFooter, isLoading }: TableProps<T>) => {
    return (
        <Paper sx={{ width: '100%', overflow: 'hidden' }}>
            <TableContainer sx={{ width: '100%', height: '100%', minHeight: 0 }}>
                <MuiTable stickyHeader>
                    <TableHead>
                        <TableRow>
                            {headData.map((item) => {
                                return <TableCell key={item}>{item}</TableCell>;
                            })}
                        </TableRow>
                    </TableHead>
                    {bodyData && bodyData.length > 0 ? (
                        <TableBody sx={{ '.MuiTableRow-root': { height: '60px' } }}>
                            {bodyData.map((item) => renderRow(item))}
                        </TableBody>
                    ) : isLoading ? (
                        <TableBody>
                            <TableRow>
                                <TableCell colSpan={headData.length}>
                                    <Stack
                                        sx={{
                                            width: '100%',
                                            alignItems: 'center',
                                            justifyContent: 'center',
                                        }}
                                    >
                                        <CircularProgress color="primary" size={30} />
                                    </Stack>
                                </TableCell>
                            </TableRow>
                        </TableBody>
                    ) : (
                        <TableBody>
                            <TableRow>
                                <TableCell colSpan={headData.length}>
                                    <Stack
                                        sx={{
                                            width: '100%',
                                            alignItems: 'center',
                                            justifyContent: 'center',
                                        }}
                                    >
                                        <img src={noData} alt="No Data" width={60} />
                                    </Stack>
                                </TableCell>
                            </TableRow>
                        </TableBody>
                    )}

                    <TableFooter>{tableFooter}</TableFooter>
                </MuiTable>
            </TableContainer>
        </Paper>
    );
};

export default Table;
