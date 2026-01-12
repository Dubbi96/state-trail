import { DeleteOutline } from '@mui/icons-material';
import { IconButton } from '@mui/material';

import { palette } from '@/themes';

type PropDeleteButton = {
    onDelete: () => void;
};
const DeleteButton: React.FC<PropDeleteButton> = ({ onDelete }) => {
    return (
        <IconButton
            aria-label="delete"
            size="small"
            onClick={(e) => {
                e.stopPropagation();
                onDelete();
            }}
        >
            <DeleteOutline sx={{ color: palette.grey[500] }} />
        </IconButton>
    );
};

export default DeleteButton;
