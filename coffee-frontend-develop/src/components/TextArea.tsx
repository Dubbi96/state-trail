import { FieldValues, Path, RegisterOptions, UseFormRegister } from 'react-hook-form';

import { TextField, TextFieldProps } from '@mui/material';

type TextAreaProps<T extends FieldValues> = Omit<TextFieldProps, 'name'> & {
    disabled?: boolean;
    register: UseFormRegister<T>;
    rules?: RegisterOptions<T, Path<T>>;
    fieldName: Path<T>;
    placeholder?: string;
};
const TextArea = <T extends FieldValues>({
    disabled = false,
    register,
    rules,
    fieldName,
    placeholder,
    ...props
}: TextAreaProps<T>) => {
    return (
        <TextField
            {...props}
            {...register(fieldName, rules)}
            disabled={disabled}
            sx={{
                'height': '100%',
                '& .MuiOutlinedInput-root': {
                    height: '100%',
                },
                ...props.sx,
            }}
            placeholder={placeholder}
            multiline
            rows={props?.rows ? props.rows : 4}
        />
    );
};

export default TextArea;
