import { Stack } from "@mui/material";

import { CircularProgress } from "@mui/material";

export const Loading = () => {
    return (
        <Stack sx={{ width: '100%', flex: 1, alignItems: 'center', justifyContent: 'center' }}>
            <CircularProgress color="primary" size={30} />
        </Stack>
    );
};